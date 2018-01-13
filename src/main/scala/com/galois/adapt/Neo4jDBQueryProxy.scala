package com.galois.adapt

import java.util.UUID

import akka.NotUsed
import akka.actor._
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.Timeout
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.CDM18
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.schema.Schema
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, RelationshipType, Result, Node => NeoNode}
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import scala.language.postfixOps

class Neo4jDBQueryProxy extends DBQueryProxyActor {

  val config: Config = ConfigFactory.load()

  val neoGraph: GraphDatabaseService = {
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

    createIfNeededUniqueConstraint(schema, "Node", "uuid")

    // NOTE: The UI expects a specific format and collection of labels on each node.
    // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

    if (config.getBoolean("adapt.ingest.producecdm")) {

      createIfNeededIndex(schema, "Subject", "timestampNanos")
      createIfNeededIndex(schema, "Subject", "cid")
      createIfNeededIndex(schema, "Subject", "cmdLine")
      createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
      createIfNeededIndex(schema, "NetFlowObject", "localAddress")
      createIfNeededIndex(schema, "NetFlowObject", "localPort")
      createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
      createIfNeededIndex(schema, "NetFlowObject", "remotePort")
      createIfNeededIndex(schema, "FileObject", "peInfo")
      createIfNeededIndex(schema, "FileObject", "path")
      createIfNeededIndex(schema, "Event", "timestampNanos")
      createIfNeededIndex(schema, "Event", "name")
      createIfNeededIndex(schema, "Event", "eventType")
      createIfNeededIndex(schema, "Event", "predicateObjectPath")

    }

    if (config.getBoolean("adapt.ingest.produceadm")) {

      createIfNeededIndex(schema, "AdmEvent", "eventType")
      createIfNeededIndex(schema, "AdmEvent", "earliestTimestampNanos")
      createIfNeededIndex(schema, "AdmEvent", "latestTimestampNanos")
      createIfNeededIndex(schema, "AdmSubject", "startTimestampNanos")
      createIfNeededIndex(schema, "AdmNetFlowObject", "localAddress")
      createIfNeededIndex(schema, "AdmNetFlowObject", "localPort")
      createIfNeededIndex(schema, "AdmNetFlowObject", "remoteAddress")
      createIfNeededIndex(schema, "AdmNetFlowObject", "remotePort")

    }

    // TODO: Neo4j doesn't want to index properties longer than 32766 bytes:
    //    createIfNeededIndex(schema, "AdmPathNode", "path")
    //    createIfNeededIndex(schema, "ADM", "originalCdmUuids")

    tx.success()
    tx.close()

    awaitSchemaCreation(graphService)
    //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)

    graphService
  }

  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))

  val shouldLogDuplicates: Boolean = config.getBoolean("adapt.ingest.logduplicates")

  def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = {
    val transaction = neoGraph.beginTx()
    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults = cdms map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(cdm.getUuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(cdmTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          verticesInThisTX += (cdm.getUuid -> newVertex)
          newVertex
        })

        cdm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k,v) => try {
            thisNeo4jVertex.setProperty(k, v)
          } catch {
            case e: Exception =>
              println(s"Tried (and failed) to set the key $k to the value $v on a Neo4j node")
              e.printStackTrace()
          }
        }

        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          if (toUuid != skipEdgesToThisUuid) verticesInThisTX.get(toUuid) match {
            case Some(toNeo4jVertex) =>
              val relationship = new RelationshipType() { def name: String = edgeName.toString }
              thisNeo4jVertex.createRelationshipTo(toNeo4jVertex, relationship)
            case None =>
              val destinationNode = Option(neoGraph.findNode(Label.label("Node"), "uuid", toUuid.toString)).getOrElse {
                verticesInThisTX(toUuid) = neoGraph.createNode(Label.label("Node"))  // Create empty node
                verticesInThisTX(toUuid)
              }
              val relationship = new RelationshipType() { def name: String = edgeName.toString }
              thisNeo4jVertex.createRelationshipTo(destinationNode, relationship)
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

  def AdmTx(adms: Seq[Either[EdgeAdm2Adm, ADM]]): Try[Unit] = {
    val transaction = neoGraph.beginTx()
    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val admToNodeResults = adms map {
      case Left(edge) => Try {

        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = verticesInThisTX
            .get(edge.src)
            .orElse(Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.src.uuid.toString)))
            .getOrElse(throw AdmInvariantViolation(edge))

          val target = verticesInThisTX
            .get(edge.tgt)
            .orElse(Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.tgt.uuid.toString)))
            .getOrElse(throw AdmInvariantViolation(edge))

          source.createRelationshipTo(target, new RelationshipType() {
            def name: String = edge.label
          })
        }
      }

      case Right(adm) => Try {
        val admTypeName = adm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(adm.uuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(admTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          verticesInThisTX += (adm.uuid.uuid -> newVertex)
          newVertex
        })

        adm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k, v) => thisNeo4jVertex.setProperty(k, v)
        }

        Some(adm.uuid)
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) println(s"Skipping duplicate creation of node: ${adm.uuid}")
          Success(None)
        case e: Throwable => Failure(e)
      }
    }

    Try {
      if (admToNodeResults.forall(_.isSuccess))
        transaction.success()
      else {
        println(s"TRANSACTION FAILURE! ADMs:\n")
        admToNodeResults foreach { case t if t.isFailure => t.failed.get.printStackTrace() }
        transaction.failure()
      }
      transaction.close()
    }
  }

  def FutureTx[T](body: =>T)(implicit ec: ExecutionContext): Future[T] = Future {
    val tx = neoGraph.beginTx()
    val result: T = body
    tx.success()
    tx.close()
    result
  }

  override def receive: PartialFunction[Any,Unit] = super.receive orElse {

    // Cypher queries are only supported by Neo4j
    case CypherQuery(q, shouldParse) =>
      println(s"Received Cypher query: $q")
      sender() ! Future {
        Try {
          val results = neoGraph.execute(q)
          if (shouldParse) {
            DBQueryProxyActor.toJson(results.asScala.toList)
          } else {
            val stringResult = results.resultAsString()
            JsString(
              stringResult
                .map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""")
                .mkString("[", ",", "]")
            )
          }
        }
      }
  }
}


object Neo4jFlowComponents {

  def neo4jActorCdmWriteSink(neoActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[CDM18, NotUsed] = Flow[CDM18]
    .collect { case cdm: DBNodeable[_] => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToNeo4jDB.apply)
    .recover{ case e: Throwable => e.printStackTrace() }
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Ack, completionMsg))(Keep.right)

  def neo4jActorAdmWriteSink(neoActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[Either[EdgeAdm2Adm, ADM], NotUsed] = Flow[Either[EdgeAdm2Adm, ADM]]
    .groupedWithin(1000, 1 second)
    .map(WriteAdmToNeo4jDB.apply)
    .recover{ case e: Throwable => e.printStackTrace() }
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Ack, completionMsg))(Keep.right)
}