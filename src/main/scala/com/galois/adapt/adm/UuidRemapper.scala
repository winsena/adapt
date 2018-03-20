package com.galois.adapt.adm

import java.io.{File, FileWriter}
import java.util.UUID
import java.util.function.BiConsumer

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import com.galois.adapt.MapDBUtils.AlmostMap
import com.galois.adapt.adm.EntityResolution.Timed
import org.mapdb.HTreeMap

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object UuidRemapper {

  // This describes the type of input that the UUID remapping stage expects to get
  sealed trait UuidRemapperInfo
  case class AnAdm(adm: ADM) extends UuidRemapperInfo
  case class AnEdge(edge: Edge) extends UuidRemapperInfo
  case class CdmMerge(merged: CdmUUID, into: CdmUUID) extends UuidRemapperInfo
  case object JustTime extends UuidRemapperInfo


  type UuidRemapperFlow = Flow[Timed[UuidRemapperInfo], Either[ADM, EdgeAdm2Adm], NotUsed]

  def apply(
    expiryTime: Long,                      // How long to hold on to a CdmUUID (waiting for a remap) until we expire it
    cdm2cdm: AlmostMap[CdmUUID, CdmUUID],  // Mapping for CDM uuids that have been mapped onto other CDM uuids
    cdm2adm: AlmostMap[CdmUUID, AdmUUID],  // Mapping for CDM uuids that have been mapped onto ADM uuids
    blocking: mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])]
  ): UuidRemapperFlow = Flow[Timed[UuidRemapperInfo]].statefulMapConcat[Either[ADM, EdgeAdm2Adm]] { () =>

    // Keep track of current time art the tip of the stream, along with things that will expire
    var currentTime: Long = 0
    var expiryTimes: Fridge[CdmUUID] = Fridge.empty


    // Add some 'CDM -> ADM' to the maps and notify those previously blocked
    def putCdm2Adm(source: CdmUUID, target: AdmUUID): List[Either[ADM, EdgeAdm2Adm]] = {
      // Yell loudly if we are about to overwrite something
      assert(!(cdm2adm contains source) || cdm2adm(source) == target,
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2adm(source)}"
      )
      assert(!(cdm2cdm contains source),
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2cdm(source)}"
      )

      cdm2adm(source) = target
      advanceAndNotify(source, blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
    }

    // Add some 'CDM -> CDM' to the maps and notify those previously blocked
    def putCdm2Cdm(source: CdmUUID, target: CdmUUID): List[Either[ADM, EdgeAdm2Adm]] = {
      // Yell loudly if we are about to overwrite something
      assert(!(cdm2adm contains source),
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2adm(source)}"
      )
      assert(!(cdm2cdm contains source) || cdm2cdm(source) == target,
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2cdm(source)}"
      )

      cdm2cdm(source) = target
      advanceAndNotify(source, blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
    }


    // Expire old UUIDs based on the current time. Note all this does is create a new node and call out to `putCdm2Adm`.
    def updateTimeAndExpireOldUuids(time: Long): List[Either[ADM, EdgeAdm2Adm]] = {
      var toReturn: mutable.ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

      if (time > currentTime) {
        // Unfortunately time information observed by UuidRemapper can be jittery since it is interacting with several "tip
        // of the streams".
        currentTime = time

        while (expiryTimes.peekFirstToExpire.exists { case (_, t) => t <= currentTime }) {
          val (keysToExpire, _) = expiryTimes.popFirstToExpire().get

          val emitted = for {
            cdmUuid <- keysToExpire
            (edges, originalCdmUuids) <- blocking.get(cdmUuid).toList

            originalCdms = originalCdmUuids.toList
            synthesizedAdm = AdmSynthesized(originalCdms)
            _ = println(s"Expired ${synthesizedAdm.uuid}")

            out <- Left(synthesizedAdm) :: originalCdms.flatMap(cdmUuid => putCdm2Adm(cdmUuid, synthesizedAdm.uuid))
          } yield out

          toReturn ++= emitted
        }
      }

      toReturn.toList
    }

    // Apply as many CDM remaps as possible
    @tailrec
    def advanceCdm(cdmUuid: CdmUUID, visited: Set[CdmUUID]): (CdmUUID, Set[CdmUUID]) = {
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid), visited | Set(cdmUuid))
    else
      (cdmUuid, visited)
    }

    // Apply as many CDM remaps as possible, then try to apply an ADM remap. If that succeeds, update all of the
    // dependent/blocked edges. Otherwise, add those edges back into the blocked map under the final CDM we had
    // advanced to.
    def advanceAndNotify(keyCdm: CdmUUID, previous: (List[Edge], Set[CdmUUID])): List[Either[ADM, EdgeAdm2Adm]] = {
      val (dependent: List[Edge], prevOriginals: Set[CdmUUID]) = previous
      val (advancedCdm: CdmUUID, originals: Set[CdmUUID]) = advanceCdm(keyCdm, prevOriginals)

      cdm2adm.get(advancedCdm) match {
        case None =>
          val (prevBlocked, prevOriginals1: Set[CdmUUID]) = blocking.getOrElse(advancedCdm, (Nil, Set.empty[CdmUUID]))
          blocking(advancedCdm) = (dependent ++ prevBlocked, originals | prevOriginals1 | Set(advancedCdm))

          // Set an expiry time, but only if there isn't one already
          if (!(expiryTimes.keySet contains advancedCdm)) {
            expiryTimes.updateExpiryTime(advancedCdm, currentTime + expiryTime)
          }

          List.empty

        case Some(adm) => dependent.flatMap(e => addEdge(e.applyRemap(Seq(keyCdm), adm)))
      }
    }

    // Advance an edge as far as possible
    def addEdge(edge: Edge): List[Either[ADM, EdgeAdm2Adm]] = edge match {
      case e: EdgeAdm2Adm => List(Right(e))
      case e@EdgeAdm2Cdm(src, lbl, tgt) => advanceAndNotify(tgt, (List(e), Set(tgt)))
      case e@EdgeCdm2Adm(src, lbl, tgt) => advanceAndNotify(src, (List(e), Set(src)))
      case e@EdgeCdm2Cdm(src, lbl, tgt) => advanceAndNotify(src, (List(e), Set(src)))
    }


    {
      // Given an ADM nodes, map all the original CDM UUIDs to ADM UUID
      case Timed(t, AnAdm(adm)) =>
        val out1 = adm.originalCdmUuids
          .flatMap(cdmUuid => if (!cdm2cdm.contains(cdmUuid)) { putCdm2Adm(cdmUuid, adm.uuid) } else { List() })
          .toList
        val out2 = updateTimeAndExpireOldUuids(t)
        Left(adm) :: out1 ++ out2

      // Add an edge
      case Timed(t, AnEdge(edge)) =>
        val out1 = addEdge(edge)
        val out2 = updateTimeAndExpireOldUuids(t)
        out1 ++ out2

      // Add information about a CDM to CDM mapping
      case Timed(t, CdmMerge(merged, into)) =>
        val out1 = putCdm2Cdm(merged, into)
        val out2 = updateTimeAndExpireOldUuids(t)
        out1 ++ out2

      // Update just the time
      case Timed(t, JustTime) =>
        if (t == Long.MaxValue) { println("UUID remapping stage is emitting all its state...") }
        updateTimeAndExpireOldUuids(t)
    }
  }
}
