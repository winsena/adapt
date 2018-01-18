package com.galois.adapt

import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.net.URI

import akka.actor._
import com.galois.adapt.cdm17._
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.galois.adapt.scepter._

import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class TinkerGraphDBQueryProxy extends DBQueryProxyActor {

  // In memory DB
  val graph: TinkerGraph = {
    val g = TinkerGraph.open()

    g.createIndex("uuid", classOf[Vertex])
    g.createIndex("pid", classOf[Vertex])

    g
  }

  var nodeIds = collection.mutable.Map.empty[UUID, Vertex]                        // All nodes in the graph
  var missingToUuid = collection.mutable.Map.empty[UUID, List[(Vertex, String)]]  // CDM edges with a missing endpoint
  var failedStatements: Int = 0                                                   // Number of failed events
  var maxFailedMsgs = 5                                                           // Limit on the length of `failedStatementsMsgs`
  var failedStatementsMsgs: List[String] = Nil                                    // First messages on failed CDM

  def findNode(key: String, value: Any): Option[Vertex] = {
    if (key == "uuid" && value.isInstanceOf[UUID]) nodeIds.get(value.asInstanceOf[UUID])
    else graph.traversal().V().has(key,value).toList.asScala.headOption
  }

  // TinkerGraph doesn't need transactions
  override def FutureTx[T](body: => T)(implicit ec: ExecutionContext) = Future { body }

  override def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = {
    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults: Seq[Try[Unit]] = cdms map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName

        // Create the node
        val props: List[Object] = ((org.apache.tinkerpop.gremlin.structure.T.label, cdmTypeName) +: cdm.asDBKeyValues)
          .flatMap { case (k, v) => List(k, v.asInstanceOf[AnyRef]) }
        assert(props.length % 2 == 0, s"Properties should have even size: $props")
        val thisVertex = graph.addVertex(props: _*)
        nodeIds += (cdm.getUuid -> thisVertex)

        // Add all edges that we can
        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          val edge = edgeName.toString
          if (toUuid != skipEdgesToThisUuid) nodeIds.get(toUuid) match {
            case Some(toVertex) => thisVertex.addEdge(edge, toVertex)
            case None => missingToUuid(toUuid) = (thisVertex, edge) :: missingToUuid.getOrElse(toUuid, List.empty)
          }
        }

        // Complete any missing edges
        missingToUuid.getOrElse(cdm.getUuid, List.empty).foreach { case (fromVertex, edgeName) =>
          val edge = edgeName.toString
          fromVertex.addEdge(edge, thisVertex)
        }

        ()
      }.recoverWith {
        case e =>
          failedStatements += 1
          if (failedStatementsMsgs.length < maxFailedMsgs) {
            failedStatementsMsgs = failedStatementsMsgs :+ e.getMessage
          }
          Failure(e)
      }
    }

    cdmToNodeResults
      .collect { case f@Failure(_) => f }
      .headOption
      .getOrElse(Success(()))
  }

  override def AdmTx(adms: Seq[Either[adm.EdgeAdm2Adm, adm.ADM]]): Try[Unit] = {

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val admToNodeResults: Seq[Try[Unit]] = adms map {
      case Left(edge) => Try {
        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = findNode("uuid", edge.src.uuid)
            .getOrElse(throw AdmInvariantViolation(edge))

          val target = findNode("uuid", edge.tgt.uuid)
            .getOrElse(throw AdmInvariantViolation(edge))

          source.addEdge(edge.label, target)

          ()
        }
      }

      case Right(adm) => Try {
        val admTypeName = adm.getClass.getSimpleName

        val props: List[Object] = ((org.apache.tinkerpop.gremlin.structure.T.label, admTypeName) +: adm.asDBKeyValues)
          .flatMap { case (k, v) => List(k, v.asInstanceOf[AnyRef]) }

        assert(props.length % 2 == 0, s"Properties should have even size: $props")
        val newNode = graph.addVertex(props: _*)
        nodeIds += (adm.uuid.uuid -> newNode)

        ()
      }
    }

    admToNodeResults
      .collect { case f@Failure(_) => f }
      .headOption
      .getOrElse(Success(()))
  }

  var storedCdmDone: Boolean = false
  var storedAdmDone: Boolean = false

  override def receive: PartialFunction[Any,Unit] = super.receive orElse {

    case CdmDone =>
      storedCdmDone = true
      self ! StartTests
      // TODO Alec: once tests start using ADM, fix this
   //   if (storedAdmDone) {
   //     self ! StartTests
   //   }

//    case AdmDone =>
   //   storedAdmDone = true
   //   if (storedCdmDone) {
   //     self ! StartTests
   //   }

    case StartTests  =>
      log.info(s"DBActor received a message to start the tests. Remaining streams.")

      var toDisplay = scala.collection.mutable.ListBuffer.empty[String]

      org.scalatest.run(new General_TA1_Tests(
        failedStatements,
        failedStatementsMsgs,
        missingToUuid.toMap,
        graph,
        Application.instrumentationSource,
        toDisplay
      ))

      // Provider specific tests
      val providerSpecificTests = Application.instrumentationSource match {
        case "clearscope" => Some(new CLEARSCOPE_Specific_Tests(graph))
        case "trace" => Some(new TRACE_Specific_Tests(graph))
        case "cadets" => Some(new CADETS_Specific_Tests(graph))
        case "faros" => Some(new FAROS_Specific_Tests(graph))
        case "theia" => Some(new THEIA_Specific_Tests(graph))
        case "fivedirections" => Some(new FIVEDIRECTIONS_Specific_Tests(graph))
        case s => { println(s"No tests for: $s"); None }
      }
      providerSpecificTests.foreach(org.scalatest.run(_))

      println(s"Total (CDM and ADM) vertices: ${graph.traversal().V().count().next()}")

      println(s"\nIf any of these test results surprise you, please email Ryan Wright and the Adapt team at: ryan@galois.com\n")

      if (toDisplay.nonEmpty) {
        println("Opening up a web browser to display nodes which failed the tests above...  (nodes are color coded)")
        Desktop.getDesktop.browse(new URI("http://localhost:8080/#" + toDisplay.mkString("&")))
      } else {
        println("If you would like to explore your data, open browser at http://localhost:8080 to use our interactive GUI.")
        println("This GUI uses (a slightly modified version of) the gremlin query language. Try executing a search query in the GUI like any of the following to get started:")
        println("    g.V().limit(50)")
        println("    g.V().has('uuid',YOUR-UUID-HERE-NOTINQUOTES)")
        println("    g.V().has('eventType','EVENT_WRITE')")
        println("    g.V().hasLabel('FileObject').limit(20)")
      }
      println("To navigate the UI, try right-clicking or double-clicking nodes")
      println("The number in the top right corner of the browser window should be the number of nodes displayed, so if you don't see anything but you have a large number, you may want to try zooming out.")
      println("")

      println("Press CTRL^C to kill the webserver")
  }

}

case object CdmDone
case object AdmDone
case object StartTests
case class DoneDevDB(graph: Option[TinkerGraph], incompleteEdgeCount: Map[UUID, List[(Vertex,String)]])

