package com.galois.adapt

import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.schema.Schema
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import akka.actor._
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.Timeout
import com.galois.adapt.cdm17.CDM17
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, RelationshipType, Node => NeoNode}
import spray.json._
import akka.pattern.ask
import com.galois.adapt.cdm17.CDM17.EdgeTypes
import com.galois.adapt.ir._

import scala.concurrent.duration._
import collection.mutable.{Map => MutableMap}
import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class Neo4jDBQueryProxy extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  val config = ConfigFactory.load()

  val neoGraph = {
    val neo4jFile: java.io.File = new java.io.File(config.getString("adapt.runtime.neo4jfile"))
    val graphService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)
    context.system.registerOnTermination(graphService.shutdown())

    def awaitSchemaCreation(g: GraphDatabaseService): Unit = {
      val tx = g.beginTx()
      val schema = g.schema()
      for(i <- schema.getIndexes.asScala) {
        var status = schema.getIndexState(i)
        while(status != Schema.IndexState.ONLINE) {
          println(i + " is " + status)
          Thread.sleep(100)
          status = schema.getIndexState(i)
        }
        println(i + " is " + status)
      }
      tx.success()
      tx.close()
    }

    def findConstraint(schema: Schema, label: Label, prop: String): Boolean = {
      val constraints = schema.getConstraints(label).asScala
      constraints.exists { c =>
        val constrainedProps = c.getPropertyKeys.asScala
        constrainedProps.size == 1 && constrainedProps.exists(_.equals(prop))
      }
    }

    def createIfNeededUniqueConstraint(schema: Schema, labelString: String, prop: String): Unit = {
      val label = Label.label(labelString)
      if(! findConstraint(schema, label, prop)) {
        Try(schema.constraintFor(label).assertPropertyIsUnique(prop).create()) match {
          case Success(_) => ()
          case Failure(e) if e.getCause.isInstanceOf[AlreadyConstrainedException] => println(s"Ignoring an already constrained label: ${label.name}")
          case Failure(e) => throw e
        }
      }
    }

    def findIndex(schema: Schema, label: Label, prop: String): Boolean = {
      val indices = schema.getIndexes(label).asScala
      indices.exists { i =>
        val indexedProps = i.getPropertyKeys.asScala
        indexedProps.size == 1 && indexedProps.exists(_.equals(prop))
      }
    }

    def createIfNeededIndex(schema: Schema, labelString: String, prop: String) = {
      val label = Label.label(labelString)
      if(! findIndex(schema, label, prop)) {
        schema.indexFor(label).on(prop).create()
      }
    }


    val tx = graphService.beginTx()
    val schema = graphService.schema()

    createIfNeededUniqueConstraint(schema, "CDM", "uuid")

    // NOTE: The UI expects a specific format and collection of labels on each node.
    // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

    createIfNeededIndex(schema, "Subject", "timestampNanos")
    createIfNeededIndex(schema, "Subject", "cid")
    createIfNeededIndex(schema, "Subject", "cmdLine")
    createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
    createIfNeededIndex(schema, "NetFlowObject", "localAddress")
    createIfNeededIndex(schema, "NetFlowObject", "localPort")
    createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
    createIfNeededIndex(schema, "NetFlowObject", "remotePort")
    createIfNeededIndex(schema, "FileObject", "peInfo")
    createIfNeededIndex(schema, "Event", "timestampNanos")
    createIfNeededIndex(schema, "Event", "name")
    createIfNeededIndex(schema, "Event", "eventType")
    createIfNeededIndex(schema, "Event", "predicateObjectPath")

    tx.success()
    tx.close()

    awaitSchemaCreation(graphService)
    //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)

    graphService
  }
  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))


  val shouldLogDuplicates = config.getBoolean("adapt.ingest.logduplicates")

  def neo4jDBNodeableTx(cdms: Seq[DBNodeable], g: GraphDatabaseService): Try[Unit] = {
    val transaction = g.beginTx()
    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults = cdms map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(cdm.getUuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = g.createNode(Label.label("CDM"), Label.label(cdmTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          verticesInThisTX += (cdm.getUuid -> newVertex)
          newVertex
        })

        cdm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k,v) => thisNeo4jVertex.setProperty(k, v)
        }

        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          if (toUuid != skipEdgesToThisUuid) verticesInThisTX.get(toUuid) match {
            case Some(toNeo4jVertex) =>
              thisNeo4jVertex.createRelationshipTo(toNeo4jVertex, edgeName)
            case None =>
              val destinationNode = Option(g.findNode(Label.label("CDM"), "uuid", toUuid.toString)).getOrElse {
                verticesInThisTX(toUuid) = g.createNode(Label.label("CDM"))  // Create empty node
                verticesInThisTX(toUuid)
              }
              thisNeo4jVertex.createRelationshipTo(destinationNode, edgeName)
          }
        }
        Some(cdm.getUuid)
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) println(s"Skipping duplicate creation of node: ${cdm.getUuid}")
          Success(None)
        //  case e: MultipleFoundException => Should never find multiple nodes with a unique constraint on the `uuid` field
        case e =>
          e.printStackTrace()
          Failure(e)
      }
    }

    Try {
      if (cdmToNodeResults.forall(_.isSuccess))
        transaction.success()
      else {
        println(s"TRANSACTION FAILURE! CDMs:\n$cdms")
        transaction.failure()
      }
      transaction.close()
    }
  }

  def neo4jIrTx(irs: Seq[Either[EdgeIr2Ir, IR]], g: GraphDatabaseService): Try[Unit] = {
    val transaction = g.beginTx()
    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val irToNodeResults = irs map {
      case Left(edge) => Try {

        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = verticesInThisTX.get(edge.src) match {
            case Some(fromNeo4jVertex) => fromNeo4jVertex
            case None => Option(g.findNode(Label.label("CDM"), "uuid", edge.src.uuid.toString)).get // TODO error handling
          }

          val target = verticesInThisTX.get(edge.tgt) match {
            case Some(toNeo4jVertex) => toNeo4jVertex
            case None => Option(g.findNode(Label.label("CDM"), "uuid", edge.tgt.uuid.toString)).get // TODO error handling
          }

          source.createRelationshipTo(target, new RelationshipType() {
            def name = edge.label
          })
        }
      }

      case Right(ir) => Try {
        val cdmTypeName = ir.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(ir.uuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = g.createNode(Label.label("CDM"), Label.label(cdmTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          verticesInThisTX += (ir.uuid.uuid -> newVertex)
          newVertex
        })

        ir.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k, v) => thisNeo4jVertex.setProperty(k, v)
        }

        Some(ir.uuid)
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) println(s"Skipping duplicate creation of node: ${ir.getUuid}")
          Success(None)
        //  case e: MultipleFoundException => Should never find multiple nodes with a unique constraint on the `uuid` field
        case e =>
          e.printStackTrace()
          Failure(e)
      }
    }

    Try {
      if (irToNodeResults.forall(_.isSuccess))
        transaction.success()
      else {
        println(s"TRANSACTION FAILURE! IRs:\n") // $irs") // The IRS is coming after you!
        irToNodeResults.find(_.isFailure) match {
          case Some(Failure(e)) => e.printStackTrace()
        }
        transaction.failure()
      }
      transaction.close()
    }
  }

  var writeToDbCounter = 0L

  def FutureTx[T](body: =>T)(implicit ec: ExecutionContext): Future[T] = Future {
      val tx = neoGraph.beginTx()
      val result: T = body
      tx.success()
      tx.close()
      result
    }



  def receive = {

    case Ready => sender() ! Ready

    case NodeQuery(q, shouldParse) =>
      println(s"Received node query: $q")
      sender() ! FutureTx {
        Query.run[Vertex](q, graph).map { vertices =>
          println(s"Found: ${vertices.length} nodes")
          if (shouldParse) JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
          else vertices
        }
      }

    case EdgeQuery(q, shouldParse) =>
      println(s"Received new edge query: $q")
      sender() ! FutureTx {
        Query.run[Edge](q, graph).map { edges =>
          println(s"Found: ${edges.length} edges")
          if (shouldParse)
            JsArray(edges.map(ApiJsonProtocol.edgeToJson).toVector)
          else
            edges
        }
      }

    // Run the given query without specifying what the output type will be. This is the variant used by 'cmdline_query.py'
    case StringQuery(q, shouldParse) =>
      println(s"Received string query: $q")
      sender() ! FutureTx {
        Query.run[java.lang.Object](q, graph).map { results =>
          println(s"Found: ${results.length} items")
          if (shouldParse) {
            toJson(results.toList)
          } else {
            JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[", ",", "]"))
          }
        }
      }

    case WriteCdmToNeo4jDB(cdms) => sender() ! neo4jDBNodeableTx(cdms, neoGraph)
    case WriteIrToNeo4jDB(irs) => sender() ! neo4jIrTx(irs, neoGraph)

//      counter = counter + cdms.size
//      log.info(s"DBActor received: $counter")

    case FailureMsg(e: Throwable) =>  log.error(s"FAILED: {}", e)
    case CompleteMsg =>
      log.info(s"DBActor received a completion message")
      sender() ! Success(())

    case InitMsg =>
      log.info(s"DBActor received an initialization message")
      sender() ! Success(())
  }

  import scala.collection.JavaConversions._

  def toJson: Any => JsValue = {

    // Numbers
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: java.lang.Long => JsNumber(n)
    case n: java.lang.Double => JsNumber(n)

    // Strings
    case s: String => JsString(s)

    // Lists
    case l: java.util.List[_] => toJson(l.toList)
    case l: List[_] => JsArray(l map toJson)

    // Maps
    case m: java.util.Map[_,_] => toJson(m.toMap)
    case m: Map[_,_] => JsObject(m map { case (k,v) => (k.toString, toJson(v)) })

    // Special cases (commented out because they are pretty verbose) and functionality is
    // anyways accessible via the "vertex" and "edges" endpoints
 //   case v: Vertex => ApiJsonProtocol.vertexToJson(v)
 //   case e: Edge => ApiJsonProtocol.edgeToJson(e)

    // Other: Any custom 'toString'
    case o => JsString(o.toString)

  }
}




sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object Ready

case class WriteCdmToNeo4jDB(cdms: Seq[DBNodeable])
case class WriteIrToNeo4jDB(irs: Seq[Either[EdgeIr2Ir, IR]])



object Neo4jFlowComponents {

  def neo4jActorCdmWrite(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToNeo4jDB.apply)
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Success(()), CompleteMsg, FailureMsg.apply))(Keep.right)

  def neo4jActorCdmWriteFlow(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToNeo4jDB.apply)
    .mapAsync(1)(msg => (neoActor ? msg).mapTo[Try[Unit]])

  def neo4jActorIrWrite(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[Either[EdgeIr2Ir, IR]]
    .groupedWithin(1000, 1 second)
    .map(WriteIrToNeo4jDB.apply)
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Success(()), CompleteMsg, FailureMsg.apply))(Keep.right)

  def neo4jActorIrWriteFlow(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[Either[EdgeIr2Ir, IR]]
    .groupedWithin(1000, 1 second)
    .map(WriteIrToNeo4jDB.apply)
    .mapAsync(1)(msg => (neoActor ? msg).mapTo[Try[Unit]])
}

case class FailureMsg(e: Throwable)
case object CompleteMsg
case object InitMsg
