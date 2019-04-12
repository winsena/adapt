package com.galois.adapt

import java.io._
import java.nio.file.Paths
import java.util.UUID
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, _}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import com.galois.adapt.adm._
import FlowComponents._
import akka.NotUsed
import akka.event.{Logging, LoggingAdapter}
import shapeless._
import shapeless.syntax.singleton._
import AdaptConfig._
import com.galois.adapt.PpmFlowComponents.CompletedESO
import com.typesafe.config.{Config, ConfigFactory}
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
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


object Application extends App {
  org.slf4j.LoggerFactory.getILoggerFactory  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.

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

  // Write stats about this run to config file passed in with `-Dconfig.file=...` if found
  Option(System.getProperty("config.file")) match {
    case None =>
      println("Failed to find a config file to which to append information about this run.")

    case Some(configFilePath) =>
      val configFileWriter = new FileWriter(configFilePath, true)

      configFileWriter.write("\n")
      configFileWriter.write("// AUTOGENERATED (when this config was used)\n")

      for ((k,v) <- summary)
        configFileWriter.write(s"//   $k $v\n")

      configFileWriter.close()
  }
  println(s"Information identifying this run:\n${summary.map(x => s"  ${x._1} ${x._2}").mkString("\n")}")

  val actorSystemGraphService: (ActorSystem, Option[GraphService[AdmUUID]]) = runFlow match {
    case "quine" =>
      val hostConfigSrcs: List[String] = quineConfig.hosts
        .zipWithIndex  
        .map { case (QuineHost(ip, _), idx) => 
          s"""|  {
              |    hostname = $ip
              |    port = 2551
              |    first-shard = ${idx * quineConfig.shardsperhost}
              |    last-shard = ${(idx + 1) * quineConfig.shardsperhost - 1}
              |  }
              |""".stripMargin
        }
    
      val clusterConfigSrc =
        s"""|name = "adapt-cluster"
            |hostname = "${quineConfig.thishost}"
            |port = 2551
            |host-shard-ranges = ${hostConfigSrcs.mkString("[\n",",","]\n")}
            |""".stripMargin

      val graphService = GraphService.clustered(
        config = ConfigFactory.parseString(clusterConfigSrc),
        persistor = as => LMDBSnapshotPersistor(
          mapSizeBytes = {
            val size: Long = runtimeConfig.lmdbgigabytes * 1024L * 1024L * 1024L
            println("LMDB size: " + size)
            size
          }
        )(as), // EmptyPersistor()(as),
        idProvider = AdmUuidProvider,
        indexer = Indexer.currentIndex(EmptyIndex),
        inMemorySoftNodeLimit = Some(100000),
        inMemoryHardNodeLimit = Some(200000),
        uiPort = None
      )
      (graphService.system, Some(graphService))

    case _ =>
      (ActorSystem("production-actor-system"), None)
  }

  implicit val system = actorSystemGraphService._1 
  val log: LoggingAdapter = Logging.getLogger(system, this)

  // All large maps should be store in `MapProxy`
  val hostNames: List[HostName] = ingestConfig.hosts.toList.map(_.hostName)
  val hostNameForAllHosts = "BetweenHosts"
  val mapProxy: MapProxy = new MapProxy(
    fileDbPath = runFlow match { case "accept" => None; case _ => admConfig.mapdb },
    fileDbBypassChecksum = admConfig.mapdbbypasschecksum,
    fileDbTransactions = admConfig.mapdbtransactions,

    admConfig.uuidRemapperShards,
    hostNames,
    hostNameForAllHosts,
    cdm2cdmLruCacheSize = admConfig.cdm2cdmlrucachesize,
    cdm2admLruCacheSize = admConfig.cdm2admlrucachesize,
    dedupEdgeCacheSize = admConfig.dedupEdgeCacheSize
  )

//    new File(this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath).setExecutable(true)
//    new File(config.getString("adapt.runtime.iforestpath")).setExecutable(true)

  var someTestFailed: Boolean = false

  val quitOnError = runtimeConfig.quitonerror
  val streamErrorStrategy: Supervision.Decider = {
    case e: Throwable =>
      e.printStackTrace()
      if (quitOnError) Runtime.getRuntime.halt(1)
      Supervision.Resume
  }
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(streamErrorStrategy))
  implicit val executionContext = system.dispatcher

  val statusActor = system.actorOf(Props[StatusActor], name = "statusActor")
//  val logFile = config.getString("adapt.logfile")
//  val scheduledLogging = system.scheduler.schedule(10.seconds, 10.seconds, statusActor, LogToDisk(logFile))
//  system.registerOnTermination(scheduledLogging.cancel())

  val ppmBaseDirFile = new File(ppmConfig.basedir)
  if ( ! ppmBaseDirFile.exists()) ppmBaseDirFile.mkdir()

  // Start up the database
  var dbActor: ActorRef = runFlow match {
    case "accept" => system.actorOf(Props(classOf[TinkerGraphDBQueryProxy]))
    case "quine" => ActorRef.noSender
    case _ => ???
  }
  val dbStartUpTimeout = Timeout(600 seconds)  // Don't make this implicit.
//  println(s"Waiting for DB indices to become active: $dbStartUpTimeout")
//  Await.result(dbActor.?(Ready)(dbStartUpTimeout), dbStartUpTimeout.duration)

  // These are the maps that `UUIDRemapper` will use
  val cdm2cdmMaps: Map[HostName, Array[AlmostMap[CdmUUID,CdmUUID]]] = mapProxy.cdm2cdmMapShardsMap
  val cdm2admMaps: Map[HostName, Array[AlmostMap[CdmUUID,AdmUUID]]] = mapProxy.cdm2admMapShardsMap

  // Edges blocked waiting for a target CDM uuid to be remapped.
  val blockedEdgesMaps: Map[HostName, Array[mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])]]] = mapProxy.blockedEdgesShardsMap

  val seenEdgesMaps: Map[HostName, Array[AlmostSet[EdgeAdm2Adm]]] = mapProxy.seenEdgesShardsMap
  val seenNodesMaps: Map[HostName, Array[AlmostSet[AdmUUID]]] = mapProxy.seenNodesShardsMap
  val uuidRemapperShardCounts: Map[HostName, Array[Long]] = hostNames.map(
    _ -> Array.fill(admConfig.uuidRemapperShards)(0L)
  ).toMap
  val dedupShardCounts: Map[HostName, Array[Long]] = (hostNameForAllHosts :: hostNames).map(
    _ -> Array.fill(admConfig.uuidRemapperShards)(0L)
  ).toMap

  // Mutable state that gets updated during ingestion
  var failedStatements: List[(Int, String)] = Nil

  val errorHandler: ErrorHandler = runFlow match {
    case "accept" => new ErrorHandler {
      override def handleError(offset: Long, error: Throwable): Unit = {
        failedStatements = (offset.toInt, error.getMessage) :: failedStatements
      }
    }
    case _ => ErrorHandler.print
  }

  val cdmSources: Map[HostName, (IngestHost, Source[(Namespace,CDM20), NotUsed])] = ingestConfig.hosts.map { host: IngestHost =>
    host.hostName -> (host, host.toCdmSource(errorHandler))
  }.toMap

  val erMap: Map[HostName, Flow[(String,CurrentCdm), Either[ADM, EdgeAdm2Adm], NotUsed]] = runFlow match {
    case "accept" => Map.empty
    case _ => ingestConfig.hosts.map { host: IngestHost =>
      host.hostName -> EntityResolution(
        admConfig,
        host,
        cdm2cdmMaps(host.hostName),
        cdm2admMaps(host.hostName),
        blockedEdgesMaps(host.hostName),
        uuidRemapperShardCounts(host.hostName),
        log,
        seenNodesMaps(host.hostName),
        seenEdgesMaps(host.hostName),
        dedupShardCounts(host.hostName)
      )
    }.toMap
  }
  val betweenHostDedup: Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed] = if (hostNames.size <= 1) {
    Flow.apply[Either[ADM, EdgeAdm2Adm]]
  } else {
    DeduplicateNodesAndEdges.apply(
      admConfig.uuidRemapperShards,
      seenNodesMaps(hostNameForAllHosts),
      seenEdgesMaps(hostNameForAllHosts),
      dedupShardCounts(hostNameForAllHosts)
    )
  }

  val ppmManagerActors: Map[HostName, ActorRef] = runFlow match {
    case "quine" =>
      ingestConfig.hosts.map { host: IngestHost =>
        val props = Props(classOf[PpmManager], host.hostName, host.simpleTa1Name, host.isWindows, actorSystemGraphService._2.get)
        val ref = system.actorOf(props, s"ppm-actor-${host.hostName}")
        host.hostName -> ref
      }.toMap + (hostNameForAllHosts -> system.actorOf(Props(classOf[PpmManager], hostNameForAllHosts, "<no-name>", false, actorSystemGraphService._2.get), s"ppm-actor-$hostNameForAllHosts"))
        // TODO nichole:  what instrumentation source should I give to the `hostNameForAllHosts` PpmManager? This smells bad...
    case _ => Map.empty
  }

  // Produce a Sink which accepts any type of observation to distribute as an observation to PPM tree actors for every host.
  def ppmObservationDistributorSink[T]: Sink[T, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val actorList: List[ActorRef] = ppmManagerActors.toList.map(_._2)
    val broadcast = b.add(Broadcast[T](actorList.size))
    actorList.foreach { ref => broadcast ~> Sink.actorRefWithAck[T](ref, InitMsg, Ack, CompleteMsg) }
    SinkShape(broadcast.in)
  })
  
  def startWebServer(): Http.ServerBinding = {
    println(s"Starting the web server at: http://${runtimeConfig.webinterface}:${runtimeConfig.port}")
    val route = Routes.mainRoute(dbActor, statusActor, ppmManagerActors)
    val httpServer = Http().bindAndHandle(route, runtimeConfig.webinterface, runtimeConfig.port)
    Await.result(httpServer, 10 seconds)
  }

  //instantiate AlarmReporter actor
  AlarmReporter

  runFlow match {

    case "accept" =>
      println("Running acceptance tests")

      val writeTimeout = Timeout(30.1 seconds)

      val sink = Sink.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val broadcast = b.add(Broadcast[(String,CDM20)](1))

        broadcast ~> DBQueryProxyActor.graphActorCdm20WriteSink(dbActor, CdmDone)(writeTimeout)
     //   broadcast ~> EntityResolution(uuidRemapper) ~> Neo4jFlowComponents.neo4jActorAdmWriteSink(dbActor, AdmDone)(writeTimeout)
        SinkShape(broadcast.in)
      })

      startWebServer()

      val handler: ErrorHandler = new ErrorHandler {
        override def handleError(offset: Long, error: Throwable): Unit = {
          failedStatements = (offset.toInt, error.getMessage) :: failedStatements
        }
      }

      assert(cdmSources.size == 1, "Cannot run tests for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallel.size == 1, "Cannot run tests for more than one linear ingest stream")
      val li = host.parallel.head

      cdmSource
        .via(printCounter("CDM events", statusActor, li.range.startInclusive))
        .recover{ case e: Throwable => e.printStackTrace(); ??? }
        .runWith(sink)


    case "database" | "db" | "ingest" =>
      val completionMsg = if (ingestConfig.quitafteringest) {
        println("Will terminate after ingest.")
        KillJVM
      } else CompleteMsg
      val writeTimeout = Timeout(30.1 seconds)

      println(s"Running database flow to ${ingestConfig.produce} with UI.")
      startWebServer()

      ingestConfig.produce match {
        case ProduceCdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val merge = b.add(Merge[(Namespace,CDM20)](sources.size))

            for (((host, source), i) <- sources.zipWithIndex) {
              source.via(printCounter(host.hostName, statusActor, 0)) ~> merge.in(i)
            }

            merge.out ~> DBQueryProxyActor.graphActorCdm20WriteSink(dbActor, completionMsg)(writeTimeout)

            ClosedShape
          }).run()

        case ProduceAdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val merge = b.add(Merge[Either[ADM, EdgeAdm2Adm]](sources.size))


            for (((host, source), i) <- sources.zipWithIndex) {
              source.via(printCounter(host.hostName, statusActor, 0)) ~> erMap(host.hostName) ~> merge.in(i)
            }

            merge.out ~> betweenHostDedup ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

            ClosedShape
          }).run()

        case ProduceCdmAndAdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val mergeCdm = b.add(Merge[(Namespace, CurrentCdm)](sources.size))
            val mergeAdm = b.add(Merge[Either[ADM, EdgeAdm2Adm]](sources.size))


            for (((host, source), i) <- sources.zipWithIndex) {
              val broadcast = b.add(Broadcast[(Namespace, CurrentCdm)](2))
              source.via(printCounter(host.hostName, statusActor, 0)) ~> broadcast.in

              broadcast.out(0)                         ~> mergeCdm.in(i)
              broadcast.out(1) ~> erMap(host.hostName) ~> mergeAdm.in(i)
            }

            mergeCdm.out                     ~> DBQueryProxyActor.graphActorCdm20WriteSink(dbActor, completionMsg)(writeTimeout)
            mergeAdm.out ~> betweenHostDedup ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

            ClosedShape
          }).run()
      }

    case "e3" | "e3-no-db" =>
      println(
        raw"""
Unknown runflow argument e3. Quitting. (Did you mean e4?)

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

    case "quine" =>
      println("Running Quine ingest.")

      implicit val graph: GraphService[AdmUUID] = actorSystemGraphService._2.get

      implicit val timeout = Timeout(30.4 seconds)

      val parallelism = quineConfig.quineactorparallelism

      val sqidFile = StandingQueryId("standing-find_ESOFile-accumulator")
      val standingFetchFileActor = system.actorOf(
      Props(
        classOf[StandingFetchActor[ESOFileInstance]],
        implicitly[Queryable[ESOFileInstance]],
        (l: List[ESOFileInstance]) => l.foreach{ eso =>
          Try {
            val id = eso.qid.get
            val namespace = graph.idProvider.customIdFromQid(id).get.namespace
            ppmManagerActors(namespace) ! eso
          }.recover{ case e => log.error(s"Sending an ESOFileInstance match failed with message: ${e.getMessage}")}
        }
      ), sqidFile.name
    )

      val sqidSrcSnk = StandingQueryId("standing-find_ESOSrcSnk-accumulator")
      val standingFetchSrcSnkActor = system.actorOf(
      Props(
        classOf[StandingFetchActor[ESOSrcSnkInstance]],
        implicitly[Queryable[ESOSrcSnkInstance]],
        (l: List[ESOSrcSnkInstance]) => l.foreach{ eso =>
          Try {
            val id = eso.qid.get
            val namespace = graph.idProvider.customIdFromQid(id).get.namespace
            ppmManagerActors(namespace) ! eso
          }.recover{ case e => log.error(s"Sending an ESOFileInstance match failed with message: ${e.getMessage}")}
        }
      ), sqidSrcSnk.name
    )

      val sqidNetwork = StandingQueryId("standing-find_ESONetwork-accumulator")
      val standingFetchNetworkActor = system.actorOf(
      Props(
        classOf[StandingFetchActor[ESONetworkInstance]],
        implicitly[Queryable[ESONetworkInstance]],
        (l: List[ESONetworkInstance]) => l.foreach{ eso =>
          Try {
            val id = eso.qid.get
            val namespace = graph.idProvider.customIdFromQid(id).get.namespace
            ppmManagerActors(namespace) ! eso
          }.recover{ case e => log.error(s"Sending an ESOFileInstance match failed with message: ${e.getMessage}")}
        }
      ), sqidNetwork.name
    )

      graph.currentGraph.standingQueryActors = graph.currentGraph.standingQueryActors +
        (sqidFile -> standingFetchFileActor) +
        (sqidSrcSnk -> standingFetchSrcSnkActor)+
        (sqidNetwork -> standingFetchNetworkActor)
      println(branchOf[ESOFileInstance]())


      val quineRouter = system.actorOf(Props(classOf[QuineRouter], parallelism, graph))
      dbActor = quineRouter

      startWebServer()
      statusActor ! InitMsg

      // Write out debug states
      val debug = new StreamDebugger("stream-buffers|", 30 seconds, 10 seconds)

      val clusterStartupDeadline: Deadline = 60.seconds.fromNow
      while ( ! graph.clusterIsReady) {
        if (clusterStartupDeadline.isOverdue()) throw new RuntimeException(s"Timeout expired while waiting for cluster hosts to become active.")
        Thread.sleep(1000)
      }

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val hostSources = cdmSources.values.toSeq

        for (((host, source), i) <- hostSources.zipWithIndex) {
          source
            .via(printCounter(host.hostName, statusActor, 0))
            .via(debug.debugBuffer(s"[${host.hostName}] 0 before ER"))
            .via(erMap(host.hostName))
            .via(debug.debugBuffer(s"[${host.hostName}] 1 after ER / before DB"))
            .mapAsyncUnordered(parallelism)(cdm => quineRouter ? cdm)
            .recover{ case x => println(s"\n\nFAILING AT END OF STREAM.\n\n"); x.printStackTrace()}
            .runWith(Sink.ignore)
        }

        ClosedShape
      }).run()


    case "pre-e4-test" =>
      startWebServer()
      statusActor ! InitMsg

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val hostSources = cdmSources.values.toSeq
        for (((host, source), i) <- hostSources.zipWithIndex) {
          source.via(printCounter(host.hostName, statusActor)) ~> Sink.ignore
        }
        ClosedShape
      }).run()

    case "e4" | "e4-no-db" =>
      val needsDb: Boolean = runFlow == "e4"

      startWebServer()
      statusActor ! InitMsg

      // Write out debug states
      val debug = new StreamDebugger("stream-buffers|", 10 minutes, 10 seconds)

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val hostSources = cdmSources.values.toSeq
        val mergeAdm = b.add(Merge[Either[ADM, EdgeAdm2Adm]](hostSources.size))

        for (((host, source), i) <- hostSources.zipWithIndex) {
          val broadcast = b.add(Broadcast[Either[ADM, EdgeAdm2Adm]](2))
          (source.via(printCounter(host.hostName, statusActor)) via debug.debugBuffer(s"[${host.hostName}] 0 before ER")) ~>
            (erMap(host.hostName) via debug.debugBuffer(s"[${host.hostName}] 1 after ER")) ~>
            broadcast.in

          val hostPpmActorRef = ppmManagerActors(host.hostName)
          (broadcast.out(0) via debug.debugBuffer(s"[${host.hostName}] 3 before PPM state accumulator")) ~>
            (PpmFlowComponents.ppmStateAccumulator via debug.debugBuffer(s"[${host.hostName}] 4 before PPM sink")) ~>
            Sink.foreach[CompletedESO](hostPpmActorRef ! _)
//            Sink.actorRefWithAck[CompletedESO](ppmManagerActors(host.hostName), InitMsg, Ack, CompleteMsg)
            Sink.ignore

          (broadcast.out(1) via debug.debugBuffer(s"[${host.hostName}] 2 before ADM merge")) ~>
            mergeAdm.in(i)
        }

        val broadcastAdm = b.add(Broadcast[Either[ADM, EdgeAdm2Adm]](if (needsDb) { 2 } else { 1 }))
        (mergeAdm.out via debug.debugBuffer(s"~ 0 after ADM merge")) ~>
          (betweenHostDedup via debug.debugBuffer(s"~ 1 after cross-host deduplicate")) ~>
          broadcastAdm.in

        val crossHostPpmActorRef = ppmManagerActors(hostNameForAllHosts)
        (broadcastAdm.out(0) via debug.debugBuffer(s"~ 3 before cross-host PPM state accumulator")) ~>
          (PpmFlowComponents.ppmStateAccumulator via debug.debugBuffer(s"~ 4 before cross-host PPM sink")) ~>
          Sink.foreach[CompletedESO](crossHostPpmActorRef ! _)
 //         Sink.actorRefWithAck[CompletedESO](ppmManagerActors(hostNameForAllHosts), InitMsg, Ack, CompleteMsg)

        if (needsDb) {
          (broadcastAdm.out(1) via debug.debugBuffer(s"~ 2 before DB sink")).via(printCounter("DB counter", statusActor)) ~>
            DBQueryProxyActor.graphActorAdmWriteSink(dbActor)
        }

        ClosedShape
      }).run()

    case "e4-no-ppm" =>

      startWebServer()
      statusActor ! InitMsg

      // Write out debug states
      val debug = new StreamDebugger("stream-buffers|", 30 seconds, 10 seconds)

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val hostSources = cdmSources.values.toSeq
        val mergeAdm = b.add(Merge[Either[ADM, EdgeAdm2Adm]](hostSources.size))

        for (((host, source), i) <- hostSources.zipWithIndex) {
          (source.via(printCounter(host.hostName, statusActor, 0)) via debug.debugBuffer(s"[${host.hostName}] 0 before ER")) ~>
            (erMap(host.hostName) via debug.debugBuffer(s"[${host.hostName}] 1 after ER")) ~>
            mergeAdm.in(i)
        }

        (mergeAdm.out via debug.debugBuffer(s"~ 0 after ADM merge")) ~>
          (betweenHostDedup via debug.debugBuffer(s"~ 1 after cross-host deduplicate")) ~>
          DBQueryProxyActor.graphActorAdmWriteSink(dbActor)

        ClosedShape
      }).run()

    case "e4-ignore" =>

      startWebServer()
      statusActor ! InitMsg

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val hostSources = cdmSources.values.toSeq
        val mergeCdm = b.add(Merge[CDM20](hostSources.size))

        for (((host, source), i) <- hostSources.zipWithIndex) {
          source.map(_._2) ~> mergeCdm.in(i)
        }

        (mergeCdm.out via printCounter("e4-ignore", statusActor, 0)) ~> Sink.ignore

        ClosedShape
      }).run()

    case "print-cdm" =>
      var i = 0
      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val sources = cdmSources.values.toSeq
        val merge = b.add(Merge[(Namespace,CDM20)](sources.size))
        val sink = b.add(Sink.ignore)

        for (((host, source), i) <- sources.zipWithIndex) {
          source ~> merge.in(i)
        }

        merge.out.via(Flow.fromFunction { cdm =>
          println(s"Record $i: ${cdm.toString}")
          i += 1
        }) ~> sink

        ClosedShape
      }).run()

    case "event-matrix" =>
      println("Producing event matrix CSVs is no longer supported")

    case "csvmaker" | "csv" =>
      val odir = pureconfig.loadConfig[String]("adapt.outdir").right.getOrElse(".")

      startWebServer()
      statusActor ! InitMsg

      ingestConfig.produce match {
        case ProduceCdm | ProduceCdmAndAdm => println("Producing CSVs from CDM is no longer supported")
        case ProduceAdm =>
          RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
            import GraphDSL.Implicits._

            val broadcast = graph.add(Broadcast[Any](9))

            assert(cdmSources.size == 1, "Cannot produce CSVs for more than once host at a time")
            val (host, cdmSource) = cdmSources.head._2

            assert(host.parallel.size == 1, "Cannot produce CSVs for more than one linear ingest stream")
            val li = host.parallel.head

            cdmSource
              .via(printCounter("DB Writer", statusActor, li.range.startInclusive, 10000))
              .via(erMap(host.hostName))
              .via(Flow.fromFunction {
                case Left(e) => e
                case Right(ir) => ir
              }) ~> broadcast.in

            broadcast.out(0).collect{ case EdgeAdm2Adm(AdmUUID(src,n), lbl, tgt) =>  src -> Map("src-name" -> n, "label" -> lbl, "target" -> tgt.rendered) } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmEdges.csv")
            broadcast.out(1).collect{ case c: AdmNetFlowObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmNetFlowObjects.csv")
            broadcast.out(2).collect{ case c: AdmEvent => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmEvents.csv")
            broadcast.out(3).collect{ case c: AdmFileObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmFileObjects.csv")
            broadcast.out(4).collect{ case c: AdmProvenanceTagNode => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmProvenanceTagNodes.csv")
            broadcast.out(5).collect{ case c: AdmSubject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmSubjects.csv")
            broadcast.out(6).collect{ case c: AdmPrincipal => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmPrincipals.csv")
            broadcast.out(7).collect{ case c: AdmSrcSinkObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmSrcSinkObjects.csv")
            broadcast.out(8).collect{ case c: AdmPathNode => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmPathNodes.csv")

            ClosedShape
          }).run()
      }

    case "ui" | "uionly" =>
      println("Staring only the UI and doing nothing else.")
      startWebServer()

    case "valuebytes" =>
      println("NOTE: this will run using CDM")

      assert(cdmSources.size == 1, "Cannot get valuebytes for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallel.size == 1, "Cannot get valuebytes for more than one linear ingest stream")
      val li = host.parallel.head

      cdmSource
        .collect{ case (_, e: Event) if e.parameters.nonEmpty => e}
        .flatMapConcat(
          (e: Event) => Source.fromIterator(
            () => e.parameters.get.flatMap( v =>
              v.valueBytes.map(b =>
                List(akka.util.ByteString(s"<<<BEGIN_LINE\t${e.uuid}\t${new String(b)}\tEND_LINE>>>\n"))
              ).getOrElse(List.empty)).toIterator
          )
        )
        .toMat(FileIO.toPath(Paths.get("ValueBytes.txt")))(Keep.right).run()


    case "uniqueuuids" =>
      println("Running unique UUID test")
      statusActor ! InitMsg

      assert(cdmSources.size == 1, "Cannot check for unique UUIDs for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallel.size == 1, "Cannot check for unique UUIDs for more than one linear ingest stream")
      val li = host.parallel.head

      cdmSource
        .via(printCounter("UniqueUUIDs", statusActor, li.range.startInclusive))
        .statefulMapConcat[(UUID,Boolean)] { () =>
        import scala.collection.mutable.{Map => MutableMap}
        val firstObservation = MutableMap.empty[UUID, CDM20]
        val ignoreUuid = new UUID(0L,0L);
        {
          case (name, c: CDM20 with DBNodeable[_]) if c.getUuid == ignoreUuid => List()
          case (name, c: CDM20 with DBNodeable[_]) if firstObservation.contains(c.getUuid) =>
            val comparison = firstObservation(c.getUuid) == c
            if ( ! comparison) println(s"Match Failure on UUID: ${c.getUuid}\nOriginal: ${firstObservation(c.getUuid)}\nThis:     $c\n")
            List()
          case (name, c: CDM20 with DBNodeable[_]) =>
            firstObservation += (c.getUuid -> c)
            List()
          case _ => List.empty
        }
      }.runWith(Sink.ignore)

    case "novelty" | "novel" | "ppm" | "ppmonly" =>
      println("Running Novelty Detection Flow")
      statusActor ! InitMsg

      assert(cdmSources.size == 1, "Cannot run novelty flow for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallel.size == 1, "Cannot run novelty flow for more than one linear ingest stream")
      val li = host.parallel.head

      cdmSource
        .via(printCounter("Novelty", statusActor, li.range.startInclusive))
        .via(erMap(host.hostName))
        .runWith(PpmFlowComponents.ppmSink)
      startWebServer()

    case other =>
      println(s"Unknown runflow argument $other. Quitting.")
      Runtime.getRuntime.halt(1)
  }

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
    override def run(): Unit = if  (!AdaptConfig.skipshutdown) {
      val patienceLevel = 48 hours
      implicit val timeout = Timeout(patienceLevel)

      println(s"Stopping ammonite...")
      replServer.stopImmediately()

      val saveF = if (ppmConfig.shouldsaveppmtrees) {
        println(s"Saving PPM trees to disk...")
        ppmManagerActors.values.toList.foldLeft(Future.successful(Ack))((a, b) => a.flatMap(_ => (b ? SaveTrees(true)).mapTo[Ack.type]))
      } else {
        Future.successful( Ack )
      }

      val shutdownF = saveF.flatMap { _ =>
        println("Shutting down the actor system")
        system.terminate()
      }.flatMap(_ => Future { mapProxy.closeSync() })

      Await.result(shutdownF, patienceLevel)
    }
  }))
}


/**
  * Utility for debugging which components are bottlenecks in a backpressured stream system. Instantiate one of these
  * per stream system, then place [[debugBuffer]] between stages to find out whether the bottleneck is upstream (in
  * which case the buffer will end up empty) or downstream (in which case the buffer will end up full).
  *
  * @param prefix prompt with which to start status update lines
  * @param printEvery how frequently to print out status updates
  * @param reportEvery how frequently should the debug buffers report their status to the [[StreamDebugger]]
  */
class StreamDebugger(prefix: String, printEvery: FiniteDuration, reportEvery: FiniteDuration)
                    (implicit system: ActorSystem, ec: ExecutionContext) {

  import java.util.concurrent.ConcurrentHashMap
  import java.util.concurrent.atomic.AtomicInteger
  import java.util.function.BiConsumer

  val bufferCounts: ConcurrentHashMap[String, Long] = new ConcurrentHashMap()

  val scheduledStreamBuffersReport = system.scheduler.schedule(printEvery, printEvery, new Runnable {
    override def run(): Unit = {
      val listBuffer = mutable.ListBuffer.empty[(String, Long)]
      bufferCounts.forEach(new BiConsumer[String, Long] {
        override def accept(key: String, count: Long): Unit = listBuffer += (key -> count)
      })
      println(listBuffer
        .sortBy(_._1)
        .toList
        .map { case (stage, count) => s"$prefix $stage: $count" }
        .mkString(s"$prefix ==== START OF STREAM-BUFFERS REPORT ====\n", "\n", s"\n$prefix ==== END OF STREAM-BUFFERS REPORT ====")
      )
    }
  })
  system.registerOnTermination(scheduledStreamBuffersReport.cancel())

  /**
    * Create a new debug buffer flow. This is just like a (backpressured) buffer flow, but it keeps track of how many
    * items are in the buffer (possibly plus one) and reports this number periodically to the object on which this
    * method is called.
    *
    * @param name what label to associate with this debug buffer (should be unique per [[StreamDebugger]]
    * @param bufferSize size of the buffer being created
    * @tparam T type of thing flowing through the buffer
    * @return a buffer flow which periodically reports stats about how full its buffer is
    */
  def debugBuffer[T](name: String, bufferSize: Int = 10000): Flow[T,T,NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit graph =>

        import GraphDSL.Implicits._

        val bufferCount: AtomicInteger = new AtomicInteger(0)

        // Write the count out to the buffer count map regularly
        system.scheduler.schedule(reportEvery, reportEvery, new Runnable {
          override def run(): Unit = bufferCounts.put(name, bufferCount.get())
        })

        // Increment the count when entering the buffer, decrement it when exiting
        val incrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.incrementAndGet(); x }))
        val buffer = graph.add(Flow[T].buffer(bufferSize, OverflowStrategy.backpressure))
        val decrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.decrementAndGet(); x }))

        incrementCount.out ~> buffer ~> decrementCount

        FlowShape(incrementCount.in, decrementCount.out)
    })
}
