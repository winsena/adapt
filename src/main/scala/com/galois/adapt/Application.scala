package com.galois.adapt

import java.io._
import java.text.NumberFormat
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, _}
import akka.stream.scaladsl._
import akka.util.Timeout
import com.galois.adapt.adm._
import FlowComponents._
import akka.NotUsed
import akka.event.Logging
import shapeless._
import shapeless.syntax.singleton._
import AdaptConfig._
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.NoveltyDetection.{Event => _, _}
import com.galois.adapt.cdm20._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}
import sys.process._
import com.rrwright.quine.runtime._
import com.rrwright.quine.language._
//import com.rrwright.quine.language.JavaObjectSerializationScheme._
import com.rrwright.quine.language.BoopickleScheme._
import scala.collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap


object Application extends App {
  org.slf4j.LoggerFactory.getILoggerFactory  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.
  runFlow match { case "quine" => (); case _ => wrongRunFlow() }


  // Open up for SSH ammonite shelling via `ssh repl@localhost -p22222`
  import ammonite.sshd._
  import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator
  val replServer = new SshdRepl(
    SshServerConfig(
      address = "localhost", // or "0.0.0.0" for public-facing shells
      port = AdaptConfig.runtimeConfig.port + 10000, // Any available port
      passwordAuthenticator = Some(AcceptAllPasswordAuthenticator.INSTANCE)
    ),
    predef = "repl.frontEnd() = ammonite.repl.FrontEnd.JLineUnix"
  )
  replServer.start()

  // Some random string that uniquely identifies this run
  val randomIdentifier: String = {
    val length = 10
    val buffer = StringBuilder.newBuilder

    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray
    val random = new java.util.Random(System.currentTimeMillis())

    for (i <- 1 until length)
      buffer.append(chars(random.nextInt(chars.length)))

    buffer.toString()
  }

  // A summary of some interesting stats about this run
  val summary: Array[(String, String)] = {
    val hostname: Try[java.net.InetAddress] = Try(java.net.InetAddress.getLocalHost)  // Host name and IP
    val date: java.util.Date = java.util.Calendar.getInstance.getTime                 // Right now
    val cores: Int = Runtime.getRuntime.availableProcessors                           // # cores available to the JVM
    val freeGb: Double = Runtime.getRuntime.totalMemory().toDouble / 1e9              // GB available to the JVM
    val commit: Try[String] = Try("git rev-parse HEAD".!!)

    Array(
      "Identifier:" -> randomIdentifier,
      "Host name:"  -> hostname.toOption.fold("could not determine host name")(_.toString),
      "Date:"       -> date.toString,
      "Cores:"      -> cores.toString,
      "Mem (GB):"   -> freeGb.toString,
      "Git commit:" -> commit.getOrElse("could not determine git commit")
    )
  }


  println(s"Information identifying this run:\n${summary.map(x => s"  ${x._1} ${x._2}").mkString("\n")}")


  // Write stats about this run to config file passed in with `-Dconfig.file=...` if found
//  Option(System.getProperty("config.file")) match {
//    case None =>
//      println("Failed to find a config file to which to append information about this run.")
//    case Some(configFilePath) =>
//      val configFileWriter = new FileWriter(configFilePath, true)
//      configFileWriter.write("\n")
//      configFileWriter.write("// AUTOGENERATED (when this config was used)\n")
//      for ((k,v) <- summary) configFileWriter.write(s"//   $k $v\n")
//      configFileWriter.close()
//  }


  val hostConfigSrcs: List[String] = quineConfig.hosts
    .zipWithIndex
    .map { case (QuineHost(ip, shardCount, _), idx) =>
      s"""|  {
          |    hostname = $ip
          |    port = 2551
          |    first-shard = ${ quineConfig.hosts.take(idx).map(_.shardcount).sum }
          |    last-shard = ${  quineConfig.hosts.take(idx).map(_.shardcount).sum + shardCount - 1 }
          |  }
          |""".stripMargin
    }

  val clusterConfigSrc =
    s"""|name = "adapt-cluster"
        |hostname = "${quineConfig.thishost}"
        |port = 2551
        |host-shard-ranges = ${hostConfigSrcs.mkString("[\n",",","]\n")}
        |""".stripMargin

  implicit val graph = GraphService.clustered(
    config = ConfigFactory.parseString(clusterConfigSrc),
    persistor = as =>
//      LMDBSnapshotPersistor(
//        "data/persistence-lmdb.db",
//        mapSizeBytes = {
//          val size: Long = runtimeConfig.lmdbgigabytes * 1024L * 1024L * 1024L
//          println("LMDB size: " + NumberFormat.getInstance().format(size))
//          size
//        }
//      )(as),
      TimelessMapDBMultimap("data/persistence-multimap_by_event.db")(as),
//    MapDBMultimap()(as),
//    EmptyPersistor()(as),
    idProvider = AdmUuidProvider,
    indexer = Indexer.currentIndex(EmptyIndex), //InMemoryIndex(Some(Set('cid, 'path)))),
    inMemorySoftNodeLimit = Some(quineConfig.inmemsoftlimit),
    inMemoryHardNodeLimit = Some(quineConfig.inmemhardlimit),
    uiPort = None
  )
  implicit val system = graph.system

  // Wait for all members of the cluster to be ready:
  val clusterStartupDeadline: Deadline = 300.seconds.fromNow
  while ( ! graph.graphIsReady) {
    if (clusterStartupDeadline.isOverdue()) throw new RuntimeException(s"Timeout expired while waiting for cluster hosts to become active.")
    Thread.sleep(1000)
  }


  val ppmBaseDirFile = new File(ppmConfig.basedir)
  if ( ! ppmBaseDirFile.exists()) ppmBaseDirFile.mkdir()


  // All large maps should be store in `MapProxy`
  val hostNames: List[HostName] = ingestConfig.hosts.map(_.hostName)
  val hostNameForAllHosts = "BetweenHosts"

  val mapProxy: MapProxy = new MapProxy(
    fileDbPath = admConfig.mapdb,
    fileDbBypassChecksum = admConfig.mapdbbypasschecksum,
    fileDbTransactions = admConfig.mapdbtransactions,

    admConfig.uuidRemapperShards,
    hostNames,
    hostNameForAllHosts,
    cdm2cdmLruCacheSize = admConfig.cdm2cdmlrucachesize,
    cdm2admLruCacheSize = admConfig.cdm2admlrucachesize,
    dedupEdgeCacheSize = admConfig.dedupEdgeCacheSize
  )

  // These are the maps that `UUIDRemapper` will use
  val cdm2cdmMaps: Map[HostName, Array[AlmostMap[CdmUUID,CdmUUID]]] = mapProxy.cdm2cdmMapShardsMap
  val cdm2admMaps: Map[HostName, Array[AlmostMap[CdmUUID,AdmUUID]]] = mapProxy.cdm2admMapShardsMap

  // Edges blocked waiting for a target CDM uuid to be remapped.
  val blockedEdgesMaps: Map[HostName, Array[mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])]]] = mapProxy.blockedEdgesShardsMap

  val seenEdgesMaps: Map[HostName, Array[AlmostSet[EdgeAdm2Adm]]] = mapProxy.seenEdgesShardsMap
  val seenNodesMaps: Map[HostName, Array[AlmostSet[AdmUUID]]] = mapProxy.seenNodesShardsMap
  val uuidRemapperShardCounts: Map[HostName, Array[Long]] = hostNames.map(_ -> Array.fill(admConfig.uuidRemapperShards)(0L)).toMap
  val dedupShardCounts: Map[HostName, Array[Long]] = (hostNameForAllHosts :: hostNames).map(_ -> Array.fill(admConfig.uuidRemapperShards)(0L)).toMap

  val erMap: Map[HostName, Flow[(String,CurrentCdm), Either[ADM, EdgeAdm2Adm], NotUsed]] = ingestConfig.hosts.map { host: IngestHost =>
    host.hostName -> EntityResolution(
      admConfig,
      host,
      cdm2cdmMaps(host.hostName),
      cdm2admMaps(host.hostName),
      blockedEdgesMaps(host.hostName),
      uuidRemapperShardCounts(host.hostName),
      Logging.getLogger(system, this),
      seenNodesMaps(host.hostName),
      seenEdgesMaps(host.hostName),
      dedupShardCounts(host.hostName)
    )
  }.toMap

//  val betweenHostDedup: Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed] = if (hostNames.size <= 1) {
//    Flow.apply[Either[ADM, EdgeAdm2Adm]]
//  } else {
//    DeduplicateNodesAndEdges.apply(
//      admConfig.uuidRemapperShards,
//      seenNodesMaps(hostNameForAllHosts),
//      seenEdgesMaps(hostNameForAllHosts),
//      dedupShardCounts(hostNameForAllHosts)
//    )
//  }



//  val ppmManagerActors: Map[HostName, ActorRef] = ingestConfig.hosts.map { host: IngestHost =>
//    val props = Props(classOf[PpmManager], host.hostName, host.simpleTa1Name, host.isWindows, graph).withDispatcher("adapt.ppm.manager-dispatcher")
//    val ref = system.actorOf(props, s"ppm-actor-${host.hostName}")
//    host.hostName -> ref
//  }.toMap // + (hostNameForAllHosts -> system.actorOf(Props(classOf[PpmManager], hostNameForAllHosts, "<no-name>", false, graph), s"ppm-actor-$hostNameForAllHosts"))

  val ppmManagers: Map[HostName, PpmManager] = ingestConfig.hosts.map(host =>
    host.hostName -> new PpmManager(host.hostName, host.simpleTa1Name, host.isWindows, graph)
  ).toMap



  val esoFileInstanceBranch = branchOf[ESOFileInstance]().asInstanceOf[DomainGraphBranch[com.rrwright.quine.language.Create]]  // TODO: this is wrong; evidence that the `Create` requirement is wrong in so many places!
  val esoSrcSinkInstanceBranch = branchOf[ESOSrcSnkInstance]().asInstanceOf[DomainGraphBranch[com.rrwright.quine.language.Create]]  // TODO: this is wrong; evidence that the `Create` requirement is wrong in so many places!
  val esoNetworkInstanceBranch = branchOf[ESONetworkInstance]().asInstanceOf[DomainGraphBranch[com.rrwright.quine.language.Create]]  // TODO: this is wrong; evidence that the `Create` requirement is wrong in so many places!
  val esoChildProcessInstanceBranch = branchOf[ChildProcess]().asInstanceOf[DomainGraphBranch[com.rrwright.quine.language.Create]]  // TODO: this is wrong; evidence that the `Create` requirement is wrong in so many places!

  val esoFileInstanceQueryable = implicitly[Queryable[ESOFileInstance]]
  val esoSrcSinkInstanceQuerable = implicitly[Queryable[ESOSrcSnkInstance]]
  val esoNetworkInstanceQueryable = implicitly[Queryable[ESONetworkInstance]]
  val esoChildProcessInstanceQueryable = implicitly[Queryable[ChildProcess]]

  val sqidHostPrefix = quineConfig.thishost.replace(".", "-")

//  implicit val stringReader = implicitly[PickleReader[String]]

//  val subjectMultiSeen = new java.util.concurrent.ConcurrentHashMap[Set[String], None.type]()
//  val predicateMultiSeen = new java.util.concurrent.ConcurrentHashMap[Set[String], None.type]()


  val filePrefixesToDrop = List("/proc", """\windows\servicing\packages""")
  

  val sqidFile = Some(StandingQueryId(sqidHostPrefix + "_standing-fetch_ESOFile-accumulator")(
    resultHandler = Some({
      case DomainNodeSubscriptionResultFetch(from, branch, assumedEdge, nodeComponents) =>
        val reconstructed = nodeComponents.toList.flatMap(esoFileInstanceQueryable.fromNodeComponents)
        val singleReconstructed = if (reconstructed.lengthCompare(1) > 0) {
          val (subStrings, predStrings) = reconstructed.foldLeft(Set.empty[String] -> Set.empty[String]){ case ((subStrings, predStrings), eso) =>
            (subStrings + eso.subject.path.path) -> (predStrings + eso.predicateObject.path.path)
          }
          val oldEso = reconstructed.head
          val newPredPath = oldEso.predicateObject.path.copy(path = predStrings.toList.sorted.mkString(","))
          newPredPath.qid = oldEso.predicateObject.path.qid
          val newSubPath = oldEso.subject.path.copy(path = subStrings.toList.sorted.mkString(","))
          newSubPath.qid = oldEso.subject.path.qid
          val newPred = oldEso.predicateObject.copy(path = newPredPath)
          newPred.qid = oldEso.predicateObject.qid
          val newSub = oldEso.subject.copy(path = newSubPath)
          newSub.qid = oldEso.subject.qid
          val newEso: ESOFileInstance = oldEso.copy(predicateObject = newPred, subject = newSub)
          newEso.qid = oldEso.qid
          List(newEso)
        } else reconstructed

        if (singleReconstructed.lengthCompare(1) > 0) println(s"WARNING: expected the ESO observation to contain only one item: $reconstructed")


//        if (reconstructed.lengthCompare(1) > 0) {
//          val subjectPaths = reconstructed.map(eso => eso.subject.path.path).toSet
//          Option(subjectMultiSeen.putIfAbsent(subjectPaths, None)) match {
//            case None => if (subjectPaths.size > 1) println(s"Subject Paths: $subjectPaths")
//            case _ => ()
//          }
//          val predicatePaths = reconstructed.map(_.predicateObject.path.path).toSet
//          Option(predicateMultiSeen.putIfAbsent(predicatePaths, None)) match {
//            case None => if (predicatePaths.size > 1) println(s"File Paths: $predicatePaths")
//            case _ => ()
//          }
//        }

//        val size = nodeComponents.toList.map(_.flatValues().size).sum
//          println(
//            s"File NodeComponents stats = List size: ${nodeComponents.toList.size} Size: $size ${
//            if (size > 20)
//            s"\nBig: ${nodeComponents.map(_.flatValues().map(x =>
//              x._1 -> x._2.left.map(q => graph.idProvider.customIdFromQid(q).get).right.map(p => new com.rrwright.quine.language.UnpickleOps(p.thisPickle).unpickleTry[String] -> p)
//            ).mkString("\n")).mkString("\n\n")}"

////            s"""BIG one's paths:
////               |  Predicates:
////               |${reconstructed.map(r => r.predicateObject.path.path -> graph.idProvider.customIdFromQid(r.predicateObject.qid.get).get).mkString("    ", "\n    ", "")}
////               |  Subjects:
////               |${reconstructed.map(r => r.subject.path.path -> graph.idProvider.customIdFromQid(r.subject.qid.get).get).mkString("    ", "\n    ", "")}""".stripMargin

//            }"
//          )
        singleReconstructed.flatMap{ recon =>
          if (filePrefixesToDrop.exists(filePrefix => recon.predicateObject.path.path.startsWith(filePrefix))) Nil else List(recon)
        }
        StandingFetches.onESOFileMatch(singleReconstructed)
    })
  ))
  val standingFetchFileActor = ActorRef.noSender

  val sqidSrcSnk = Some(StandingQueryId(sqidHostPrefix + "_standing-fetch_ESOSrcSnk-accumulator")(
    resultHandler = Some({
      case DomainNodeSubscriptionResultFetch(from, branch, assumedEdge, nodeComponents) =>
        val reconstructed = nodeComponents.toList.flatMap(esoSrcSinkInstanceQuerable.fromNodeComponents)
        val singleReconstructed = if (reconstructed.lengthCompare(1) > 0) {  // Combine subject names so that we get a consistent name for the process. SrcSink has not predicate object path.
          val subStrings = reconstructed.foldLeft(Set.empty[String]) { case (subStrings, eso) =>
            subStrings + eso.subject.path.path
          }
          val oldEso = reconstructed.head
          val newSubPath = oldEso.subject.path.copy(path = subStrings.toList.sorted.mkString(","))
          newSubPath.qid = oldEso.subject.path.qid
          val newSub = oldEso.subject.copy(path = newSubPath)
          newSub.qid = oldEso.subject.qid
          reconstructed.map{oldEso =>
            val newEso = oldEso.copy(subject = newSub)
            newEso.qid = oldEso.qid
            newEso
          }.distinct // Remove duplicate entries since subject names have been unified.
        } else reconstructed

        if (singleReconstructed.lengthCompare(1) > 0) println(s"WARNING: expected the ESO observation to contain only one item: $reconstructed")

        StandingFetches.onESOSrcSinkMatch(singleReconstructed)
    })
  ))
  val standingFetchSrcSnkActor = ActorRef.noSender

  val sqidNetwork = Some(StandingQueryId(sqidHostPrefix + "_standing-fetch_ESONetwork-accumulator")(
    resultHandler = Some({
      case DomainNodeSubscriptionResultFetch(from, branch, assumedEdge, nodeComponents) =>
        val reconstructed = nodeComponents.toList.flatMap(esoNetworkInstanceQueryable.fromNodeComponents)
        val singleReconstructed = if (reconstructed.lengthCompare(1) > 0) {  // Combine subject names so that we get a consistent name for the process. Do not combine network objects.
          val subStrings = reconstructed.foldLeft(Set.empty[String]) { case (subStrings, eso) =>
            subStrings + eso.subject.path.path
          }
          val oldEso = reconstructed.head
          val newSubPath = oldEso.subject.path.copy(path = subStrings.toList.sorted.mkString(","))
          newSubPath.qid = oldEso.subject.path.qid
          val newSub = oldEso.subject.copy(path = newSubPath)
          newSub.qid = oldEso.subject.qid
          reconstructed.map{ oldEso =>
            val newEso = oldEso.copy(subject = newSub)
            newEso.qid = oldEso.qid
            newEso
          }.distinct // Remove duplicate entries since subject names have been unified.
        } else reconstructed

        // This _network_ ESO list is the only case where there could potentially be multiple items legitimately left in the list.
        StandingFetches.onESONetworkMatch(singleReconstructed)
    })
  ))
  val standingFetchNetworkActor = ActorRef.noSender

  val sqidParentProcess = Some(StandingQueryId(sqidHostPrefix + "_standing-fetch_ProcessParentage")(
    resultHandler = Some({
      case DomainNodeSubscriptionResultFetch(from, branch, assumedEdge, nodeComponents) =>
        val reconstructed = nodeComponents.toList.flatMap(esoChildProcessInstanceQueryable.fromNodeComponents)
        val singleReconstructed = if (reconstructed.lengthCompare(1) > 0) {  // Combine subject names so that we get a consistent name for all processes.
          val (childStrings, parentStrings) = reconstructed.foldLeft(Set.empty[String] -> Set.empty[String]) { case ((childStrings, parentStrings), eso) =>
            (childStrings + eso.path.path) -> (parentStrings + eso.parentSubject.path.path)
          }
          val oldChild = reconstructed.head
          val newChildPath = oldChild.path.copy(path = childStrings.toList.sorted.mkString(","))
          newChildPath.qid = oldChild.path.qid
          val oldParent = oldChild.parentSubject
          val newParentPath = oldParent.path.copy(path = parentStrings.toList.sorted.mkString(","))
          newParentPath.qid = oldParent.path.qid
          val newParent = oldParent.copy(path = newParentPath)
          newParent.qid = oldParent.qid
          val newChild = oldChild.copy(path = newChildPath, parentSubject = newParent)
          newChild.qid = oldChild.qid
          List(newChild)
        } else reconstructed

        if (singleReconstructed.lengthCompare(1) > 0) println(s"WARNING: expected the ESO observation to contain only one item: $singleReconstructed")
        StandingFetches.onESOProcessMatch(singleReconstructed)
    })
  ))
  val standingFetchProcessParentageActor = ActorRef.noSender

  graph.currentGraph.standingQueryActors = graph.currentGraph.standingQueryActors +
    (sqidFile.get -> standingFetchFileActor) +
    (sqidSrcSnk.get -> standingFetchSrcSnkActor) +
    (sqidNetwork.get -> standingFetchNetworkActor) +
    (sqidParentProcess.get -> standingFetchProcessParentageActor)


  val parallelism = quineConfig.quineactorparallelism
  val uiDBInterface = system.actorOf(Props(classOf[QuineDBActor], graph, -1), s"QuineDB-UI")
//    system.actorOf(Props(classOf[QuineRouter], parallelism, graph))

  AlarmReporter  // instantiate AlarmReporter (LazyInit) and corresponding actor
  StandingFetches  // Initialize object.

  val statusActor = system.actorOf(Props[StatusActor], name = "statusActor")
//  val logFile = config.getString("adapt.logfile")
//  val scheduledLogging = system.scheduler.schedule(10.seconds, 10.seconds, statusActor, LogToDisk(logFile))
//  system.registerOnTermination(scheduledLogging.cancel())

  val streamErrorStrategy: Supervision.Decider = { e: Throwable =>
    e.printStackTrace()
    if (runtimeConfig.quitonerror) Runtime.getRuntime.halt(1)
    Supervision.Resume
  }
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(streamErrorStrategy)
      .withDispatcher("stream-dispatcher")
  )


  def startWebServer(dbActor: ActorRef): Http.ServerBinding = {
    println(s"Starting the web server at: http://${runtimeConfig.webinterface}:${runtimeConfig.port}")
    val route = Routes.mainRoute(dbActor, statusActor, ppmManagers)
    val httpServer = Http().bindAndHandle(route, runtimeConfig.webinterface, runtimeConfig.port)
    Await.result(httpServer, 10 seconds)
  }

  startWebServer(uiDBInterface)
  statusActor ! InitMsg




  val lruDedup = Flow[Either[ADM, EdgeAdm2Adm]].statefulMapConcat{ () =>
    // This is meant only to decrease the number of duplicates, not perfectly eliminate dupes. Viz. meant to help performance, not correctness.
    // Some duplication isn't so bad--it results in a no-op on the graph, and a duplicate standing fetch.
    val seenPaths = new java.util.LinkedHashMap[AdmPathNode, None.type](500, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[AdmPathNode, None.type]) = this.size() >= 500
    }
    val seenSubjects = new java.util.LinkedHashMap[AdmSubject, None.type](200, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[AdmSubject, None.type]) = this.size() >= 200
    }
    val seenPorts = new java.util.LinkedHashMap[AdmPort, None.type](200, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[AdmPort, None.type]) = this.size() >= 200
    }
    val seenAddresses = new java.util.LinkedHashMap[AdmAddress, None.type](200, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[AdmAddress, None.type]) = this.size() >= 200
    }
    val seenNetflows = new java.util.LinkedHashMap[AdmNetFlowObject, None.type](200, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[AdmNetFlowObject, None.type]) = this.size() >= 200
    }
    val seenEdges = new java.util.LinkedHashMap[EdgeAdm2Adm, None.type](10000, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[EdgeAdm2Adm, None.type]) = this.size() >= 10000
    }

    {
      case a @ Left(adm: AdmPathNode) =>
        val result = if (seenPaths.containsKey(adm)) Nil else {
//          println(s"Received path node: ${adm.path}")
          List(a)
        }
        seenPaths.put(adm, None)
        result

      case a @ Left(adm: AdmSubject) =>
        val result = if (seenSubjects.containsKey(adm)) Nil else List(a)
        seenSubjects.put(adm, None)
        result

      case a @ Left(adm: AdmPort) =>
        val result = if (seenPorts.containsKey(adm)) Nil else List(a)
        seenPorts.put(adm, None)
        result

      case a @ Left(adm: AdmAddress) =>
        val result = if (seenAddresses.containsKey(adm)) Nil else List(a)
        seenAddresses.put(adm, None)
        result

      case a @ Left(adm: AdmNetFlowObject) =>
        val result = if (seenNetflows.containsKey(adm)) Nil else List(a)
        seenNetflows.put(adm, None)
        result

      case a @ Right(edge) =>
        val result = if (seenEdges.containsKey(edge)) Nil else List(a)
        seenEdges.put(edge, None)
        result

      case a => List(a)
    }
  }

  // One of `Either[ADM, EdgeAdm2Adm]`, `PpmObservation`
  def quineSink(hostName: HostName): Sink[Any, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit q: GraphDSL.Builder[NotUsed]  =>
    import GraphDSL.Implicits._
    val balance = q.add(Balance[Any](parallelism))
    (0 until parallelism).foreach { idx =>
      val quineDBRef = system.actorOf(Props(classOf[QuineDBActor], graph, idx), s"$hostName-QuineDB-$idx")
      balance.out(idx) ~> Sink.actorRefWithAck(quineDBRef, InitMsg, Ack, CompleteMsg, println).async
    }
    SinkShape(balance.in)
  })


  val standingFetchSinks = new ConcurrentHashMap[AdaptConfig.HostName, ActorRef]().asScala


  println("Running Quine ingest.")

  val cdmSources: Map[HostName, (IngestHost, Source[(Namespace,CDM20), NotUsed])] = ingestConfig.hosts.map { host: IngestHost =>
    host.hostName -> (host, host.toCdmSource(ErrorHandler.print))
  }.toMap
  val hostSources = cdmSources.values.toSeq
  val debug = new StreamDebugger("stream-buffers|", 30 seconds, 10 seconds)
  for ((host, source) <- hostSources) {
    val standingFetchSink: ActorRef = RunnableGraph.fromGraph(GraphDSL.create(
      Source.actorRef[PpmObservation](quineConfig.ppmobservationbuffer,

        OverflowStrategy.dropNew
//        OverflowStrategy.fail

    )
    ) { implicit b => standingFetchSource =>
      import GraphDSL.Implicits._

      val merge = b.add(MergePreferred[Any](1, eagerComplete = true))

      source.async
        .via(printCounter(host.hostName+" CDM", statusActor))
        .via(debug.debugBuffer(s"[${host.hostName}]  0.) before ER", 50000)).async
        .via(erMap(host.hostName).async).async
        .via(lruDedup)
        .via(debug.debugBuffer(s"[${host.hostName}]  1.) after ER / before DB", 50000)).async
        .via(printCounter(host.hostName+" ADM", statusActor)) ~> merge.in(0)

      standingFetchSource ~> merge.preferred

      merge.out.via(printCounter(host.hostName + " Quine", statusActor)) ~> quineSink(host.hostName).async

      ClosedShape
    }).run()

    standingFetchSinks += (host.hostName -> standingFetchSink)
  }


//  def addNewIngestStream(host: IngestHost): Unit = ???

//  def terminateIngestStream(hostName: HostName): Unit = ???



  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
    override def run(): Unit = if  (!AdaptConfig.skipshutdown) {
      implicit val timeout = Timeout(48 hours)

      println(s"Stopping ammonite...")
      replServer.stopImmediately()

      implicit val executionContext: ExecutionContext = system.dispatchers.lookup("quine.actor.node-dispatcher")

      val saveF = if (ppmConfig.shouldsaveppmtrees) {
        println(s"Saving PPM trees to disk...")
        ppmManagers.values.toList.foldLeft(Future.successful( () ))((a, b) =>
          a.flatMap(_ => b.saveTrees()) // (b ? SaveTrees(true)).mapTo[Ack.type]))
        )
      } else {
        Future.successful( Ack )
      }

      val shutdownF = saveF.flatMap { _ =>
        println("Shutting down the actor system")
        system.terminate()
      }.flatMap(_ => Future { mapProxy.closeSync() })

      Await.result(shutdownF, timeout.duration)
    }
  }))


  def wrongRunFlow(): Unit = {
    println(
      raw"""
Unknown runflow argument. Quitting. (Did you mean: quine?)

                \
                 \

                        _.-;:q=._
                      .' j=""^k;:\.
                     ; .F       ";`Y
                    ,;.J_        ;'j
                  ,-;"^7F       : .F           _________________
                 ,-'-_<.        ;gj. _.,---""''               .'
                ;  _,._`\.     : `T"5,                       ;
                : `?8w7 `J  ,-'" -^q. `                     ;
                 \;._ _,=' ;   n58L Y.                     .'
                   F;";  .' k_ `^'  j'                     ;
                   J;:: ;     "y:-='                      ;
                    L;;==      |:;   jT\                  ;
                    L;:;J      J:L  7:;'       _         ;
                    I;|:.L     |:k J:.' ,  '       .     ;
                     ;J:.|     ;.I F.:      .           :
                   ;J;:L::     |.| |.J  , '   `    ;    ;
                 .' J:`J.`.    :.J |. L .    ;         ;
                ;    L :k:`._ ,',j J; |  ` ,        ; ;
              .'     I :`=.:."_".'  L J             `.'
            .'       |.:  `"-=-'    |.J              ;
        _.-'         `: :           ;:;           _ ;
    _.-'"             J: :         /.;'       ;    ;
  ='_                  k;.\.    _.;:Y'     ,     .'
     `"---..__          `Y;."-=';:='     ,      .'
              `""--..__   `"==="'    -        .'
                       ``""---...__    itz .-'
                                   ``""---'

"""
    )
    Runtime.getRuntime.halt(1)
  }
}

