package com.galois.adapt

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}

import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.galois.adapt.cdm17.CDM17
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.structure.{Direction}
import org.neo4j.graphdb
import org.neo4j.graphdb.{GraphDatabaseService, Label}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.api.proc.Neo4jTypes.RelationshipType
import org.neo4j.graphdb.schema.Schema

import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


object Neo4jFlowComponents {

  val config = ConfigFactory.load()

  val threadPool = config.getInt("adapt.ingest.parallelism")

  /* Open a Neo4j graph database and create indices */
  val graph: GraphDatabaseService = {
    val neo4jFile: java.io.File = new java.io.File(config.getString("adapt.runtime.neo4jfile"))
    val graph = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)

    Try (
      graph.beginTx()
    ) match {
      case Success(tx) =>
        val schema = graph.schema()

        createIfNeededUniqueConstraint(schema, "CDM17", "uuid")

        createIfNeededIndex(schema, "Subject", "timestampNanos")
        createIfNeededIndex(schema, "Subject", "cid")
        createIfNeededIndex(schema, "Subject", "cmdLine")
        createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
        createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
        createIfNeededIndex(schema, "Event", "timestampnanos")
        createIfNeededIndex(schema, "Event", "name")
        createIfNeededIndex(schema, "Event", "predicateObjectPath")

        tx.success()
        tx.close()

        awaitSchemaCreation(graph)

        //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)
      case Failure(err) => ()
    }

    graph
  }

  def awaitSchemaCreation(graph: GraphDatabaseService) = {
    Try (
      graph.beginTx()
    ) match {
      case Success(tx) =>
        val schema = graph.schema()

        for(i <- schema.getIndexes.asScala) {
          println(i + " is " + schema.getIndexState(i))
          while(schema.getIndexState(i) != Schema.IndexState.ONLINE) {
            Thread.sleep(100)
            println(i + " is " + schema.getIndexState(i))
          }
        }

        tx.success()
        tx.close()
    }
  }

  def findConstraint(schema: Schema, label: Label, prop: String): Boolean = {
    val constraints = schema.getConstraints(label).asScala
    for(c <- constraints) {
      val constrainedProps = c.getPropertyKeys.asScala
      if(constrainedProps.size == 1 && constrainedProps.count(_.equals(prop)) == 1) {
        return true
      }
    }
    false
  }

  def createIfNeededUniqueConstraint(schema: Schema, slabel: String, prop: String) = {
    val label = Label.label(slabel)
    if(! findConstraint(schema, label, prop)) {
      schema.constraintFor(label).assertPropertyIsUnique(prop).create()
    }
  }

  def findIndex(schema: Schema, label: Label, prop: String): Boolean = {
    val indices = schema.getIndexes(label).asScala
    for(i <- indices) {
      val indexedProps = i.getPropertyKeys.asScala
      if(indexedProps.size == 1 && indexedProps.count(_.equals(prop)) == 1)
        return true
    }
    return false
  }

  def createIfNeededIndex(schema: Schema, slabel: String, prop: String) = {
    val label = Label.label(slabel)
    if(! findIndex(schema, label, prop)) {
      schema.indexFor(label).on(prop).create()
    }
  }

  // Create a neo4j transaction to insert a batch of objects
  def neo4jTx(cdms: Seq[DBNodeable]): Try[Unit] = {
    val transaction = graph.beginTx()

    // For the duration of the transaction, we keep a 'Map[UUID -> org.neo4j.graphdb.Node]' of vertices created
    // during this transaction (since we don't look those up in the usual manner).
    val newVertices = MutableMap.empty[UUID, org.neo4j.graphdb.Node]

    // We also need to keep track of edges that point to nodes we haven't found yet (this lets us
    // handle cases where nodes are out of order).
    var missingToUuid = MutableMap.empty[UUID, Set[(org.neo4j.graphdb.Node, cdm17.CDM17.EdgeTypes.EdgeTypes)]]

    // Accordingly, we define a function which lets us look up a vertex by UUID - first by checking
    // the 'newVertices' map, then falling back on a query to Neo4j.
    def findNode(uuid: UUID): Option[org.neo4j.graphdb.Node] = newVertices.get(uuid) orElse {
      None
    }

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    for (cdm <- cdms) {
      Try {
        val newNeo4jVertex = graph.createNode()
        for(label <- cdm.getLabels) {
          newNeo4jVertex.addLabel(Label.label(label))
        }
        for ((k,v) <- cdm.asDBKeyValues) {
          if(classOf[java.util.UUID] != v.getClass)
            newNeo4jVertex.setProperty(k, v)
          else
            newNeo4jVertex.setProperty(k, v.toString)
        }
        newVertices += (cdm.getUuid -> newNeo4jVertex)

        for ((label, toUuid) <- cdm.asDBEdges) {
          if (toUuid == skipEdgesToThisUuid) Success(())
          else {
            findNode(toUuid) match {
            case Some(toNeo4jVertex) =>
              newNeo4jVertex.createRelationshipTo(toNeo4jVertex, label)
            case None =>
              missingToUuid(toUuid) = missingToUuid.getOrElse(toUuid, Set[(org.neo4j.graphdb.Node, cdm17.CDM17.EdgeTypes.EdgeTypes)]()) + (newNeo4jVertex -> label)
            }
          }
        }
      } match {
        case Success(_) =>
        case Failure(e: org.neo4j.graphdb.ConstraintViolationException) =>
          if (!e.getMessage.contains("uuid")) {
            println("Failed CDM statement: " + cdm)
            println(e.getMessage) // Bad query
            e.printStackTrace()
          }
        case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
      }
    }

    println("here!")

    /*// Try to complete missing edges. If the node pointed to is _still_ not found, we
    // synthetically create it.
    var nodeCreatedCounter = 0
    var edgeCreatedCounter = 0

    for ((uuid, edges) <- missingToUuid) {

    for ((fromNeo4jVertex, label) <- edges) {
      if (uuid != skipEdgesToThisUuid) {
        // Find or create the missing vertex (it may have been created earlier in this loop)
        val toNeo4jVertex = /*findNode(uuid) getOrElse*/ {
          nodeCreatedCounter += 1
          val newNode = graph.createNode(Label.label("CDM17"))
          //newNode.setProperty("uuid", UUID.randomUUID()) // uuid)
          newVertices += (uuid -> newNode)
          newNode
        }

        // Create the missing edge
        Try {
          fromNeo4jVertex.createRelationshipTo(toNeo4jVertex, label)
          edgeCreatedCounter += 1
        } match {
          case Success(_) =>
          case Failure(e: java.lang.IllegalArgumentException) =>
            if (!e.getMessage.contains("byUuidUnique")) {
              println(e.getMessage) // Bad query
              e.printStackTrace()
            }
          case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
        }
      }
    }}*/

    Try {
          transaction.success()
          transaction.close()
        } match {
      case Success(_) => Success(())
      case Failure(e) => Failure(e)
    }
  }

  // Loop indefinetly over locking failures
  def retryFinalLoop(cdms: Seq[DBNodeable]): Boolean = {
    neo4jTx(cdms) match {
      case Success(_) => false
      case Failure(f) => throw f.getCause.getCause
    }
  }

  // Loops on insertion until we either insert the entire batch
  // A single statement may fail to insert but only after ln(batch size)+1 attempts
  // All ultimate failure errors are collected in the sequence and surfaced to the top.
  // If all statements insert successfully eventually then Seq(Success(())) is returned.
  def neo4jLoop(cdms: Seq[DBNodeable]): Seq[Try[Unit]] = {
    neo4jTx(cdms) match {
      case Success(()) => Seq(Success(()))
      case Failure(_) =>
        // If we're trying to ingest a single CDM statement, try it one more time before giving up
        if (cdms.length == 1) {
          // Loop indefinetly over locking failures
          Try {
                while (retryFinalLoop(cdms)) {
                  Thread.sleep(scala.util.Random.nextInt(100))
                }
          } match {
            case Failure(f) =>
              // If we saw a non-locking failure, try one more time to insert before reporting failure
              println("Final retry for statement")
              Seq(neo4jTx(cdms))
            case _ => Seq(Success())
          }
        } else {
          // Split the list of CDM objects in half (less likely to have object contention for each half of the list) and loop on insertion
          val (front, back) = cdms.splitAt(cdms.length / 2)
          neo4jLoop(front) match {
            case Seq(Success(_)) => neo4jLoop(back)
            case fails1 => neo4jLoop(back) match {
              case Seq(Success(_)) => fails1
              case fails2 => fails1++fails2
            }
          }
        }
    }
  }

  /* Given a 'GraphDatabaseService', make a 'Flow' that writes CDM data into that graph in a buffered manner
   */
  def neo4jWrites(graph: GraphDatabaseService = graph)(implicit ec: ExecutionContext) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 seconds)
    .mapAsyncUnordered(threadPool)(x => Future {neo4jLoop(x)})
    .toMat(
      Sink.foreach{ sOrF =>
        sOrF match {
          case Seq(Success(())) => ()
          case fails => println(s"${fails.length} insertion errors in batch")
        }
      }
    )(Keep.right)
}

