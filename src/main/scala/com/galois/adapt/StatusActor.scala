package com.galois.adapt

import java.io.{FileOutputStream, PrintWriter}

import akka.actor.Actor
import spray.json.JsObject

import scala.collection.mutable
import scala.util.Try

class StatusActor extends Actor {

  var currentlyIngesting = false

  val generalRecords = mutable.Map.empty[String,Any]

  val totalPopulationLog = mutable.Map.empty[String, Long]
  var recentPopulationLog = Map.empty[String, Long]

  def calculateStats(): StatusReport = {
    generalRecords += ("DuplicateAlarms" -> duplicateAlarmCount)
    StatusReport(
      currentlyIngesting,
      generalRecords.toMap.mapValues(_.toString),
      totalPopulationLog.toMap,
      recentPopulationLog
    )
  }

  var duplicateAlarmCount = 0L


  def receive = {
    case GetStats => sender() ! calculateStats()

    case LogToDisk(p: String) => Try {
      import ApiJsonProtocol._
      import spray.json._

      val logFile = new PrintWriter(new FileOutputStream(new java.io.File(p), true))
      logFile.append(calculateStats().toJson.compactPrint + "\n")
      logFile.close()
    }

    case IncrementAlarmDuplicateCount => duplicateAlarmCount += 1

    case p: PopulationLog =>
      p.counter.foreach{ case (k,v) =>
        val key = s"${p.name}: $k"
        totalPopulationLog += (key -> (totalPopulationLog.getOrElse(key, 0L) + v))
      }
      recentPopulationLog = p.counter

      generalRecords += ("every" -> p.every)
      generalRecords += ("secondsThisEvery" -> p.secondsThisEvery)
      generalRecords += ("blockEdgesCount" -> p.blockEdgesCount)
      generalRecords += ("blockingNodes" -> p.blockingNodes)
      generalRecords += ("uuidsBlocking" -> p.blockingNodes)
      generalRecords += ("blockedUuidResponses" -> p.blockingNodes)
      generalRecords += ("activeEventChains" -> p.activeEventChains)
      generalRecords += ("cdm2cdmSize" -> p.cdm2cdmSize)
      generalRecords += ("cdm2admSize" -> p.cdm2admSize)
      generalRecords += ("seenNodesSize" -> p.seenNodesSize)
      generalRecords += ("seenEdgesSize" -> p.seenEdgesSize)
      generalRecords += ("monotonicTime" -> p.currentTime)
      generalRecords += ("sampledTime" -> p.sampledTime)

    case InitMsg => currentlyIngesting = true
    case CompleteMsg => currentlyIngesting = false
  }
}

case object GetStats
case class LogToDisk(path: String)

case class PopulationLog(
  name: String,
  position: Long,
  every: Int,
  counter: Map[String,Long],
  secondsThisEvery: Double,
  blockEdgesCount: Long,
  blockingNodes: Long,
  uuidsBlocking: Int,
  blockedUuidResponses: Int,
  activeEventChains: Long,
  cdm2cdmSize: Long,
  cdm2admSize: Long,
  seenNodesSize: Long,
  seenEdgesSize: Long,
  currentTime: Long,
  sampledTime: Long
)

case class IncrementCount(name: String)
case class DecrementCount(name: String)

case class StatusReport(
  currentlyIngesting: Boolean,
  generalRecords: Map[String,String],
  totalPopulation: Map[String, Long],
  recentPopulation: Map[String, Long]
)

case object IncrementAlarmDuplicateCount