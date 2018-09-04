package com.galois.adapt

import java.util.UUID

import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_FORK, EVENT_LSEEK, EVENT_MMAP, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}
import org.neo4j.cypher.internal.frontend.v2_3.ast.Collection

import scala.reflect.ClassTag

trait Element
trait ComposedActivity

object SummaryParser {

  // Count
  def countActivities[A<:Element](l:List[Element], f:A=>Boolean = (_:A)=>true)(implicit tag: ClassTag[A]): Int = l.count{case a:A if f(a)=> true; case _ => false}
  def filter[A<:Element](l:List[Element], f:A=>Boolean = (_:A)=>true)(implicit tag: ClassTag[A]): List[A] = l.collect{case a:A if f(a)=> a}

  // elementary activities
  def isNWRead(a:ProcessNWActivity) = a.event == EVENT_RECVFROM
  def isNWWrite(a:ProcessNWActivity) = a.event == EVENT_WRITE
  def isFileWrite(a:ProcessFileActivity) = a.event == EVENT_WRITE
  def isFileRead(a:ProcessFileActivity) = a.event == EVENT_READ || a.event == EVENT_MMAP
  def isProcessFork(a:ProcessProcessActivity) = a.event == EVENT_FORK

  //def countFileReads(l:List[Element]): Int = {countActivities[ProcessFileActivity](l, _.event==EVENT_READ)}
//  def countFileWrites(l:List[Element]): Int = {countActivities[ProcessFileActivity](l, _.event==EVENT_WRITE)}
//  def countFileActivities(l:List[Element]): Int = {countActivities[ProcessFileActivity](l)}
//  def countNWActivities(l:List[Element]): Int = {countActivities[ProcessNWActivity](l)}
//  def countProcessActivities(l:List[Element]): Int = {countActivities[ProcessActivity](l)}
//  def countUniqueUuids(l:List[ProcessActivity]): Int = {l.map(_.subject.uuid).toSet.size}
  def countFileReads(l:List[Element]): Int = countActivities(l, isFileRead)
  def countFileWrites(l:List[Element]): Int = countActivities(l, isFileWrite)
  def countFileActivities(l:List[Element]): Int = {countActivities[ProcessFileActivity](l)}
  def countNWActivities(l:List[Element]): Int = {countActivities[ProcessNWActivity](l)}
  def countProcessActivities(l:List[Element]): Int = {countActivities[ProcessActivity](l)}

  def countUniqueUuids(l:List[ProcessActivity]): Int = {l.map(_.subject.uuid).toSet.size}

  def getProcessActivities(l:List[Element]): List[ProcessActivity] = filter(l, (_:ProcessActivity)=> true)
  //Sub-Activities
  def getFilesReadActivities(l:List[Element]): List[ProcessFileActivity] = filter(l, isFileRead)
  def getFilesWrittenActivities(l:List[Element]): List[ProcessFileActivity] = filter(l, isFileWrite)
  def getNWReadsActivities(l:List[Element]): List[ProcessNWActivity] = filter(l, isNWRead)
  def getNWWritesActivities(l:List[Element]): List[ProcessNWActivity] = filter(l, isNWWrite)
  def getProcessForkActivities(l:List[Element]): List[ProcessProcessActivity] = filter(l, isProcessFork)

  // Unique predicate objects
  def getUuids(l:List[Element]): Set[UUID] = getProcessActivities(l).map(_.subject.uuid).toSet

  def getFilesRead(l:List[Element]): Set[FilePath] = getFilesReadActivities(l).map(_.filePath).toSet
  def getFilesWritten(l:List[Element]): Set[FilePath] = getFilesWrittenActivities(l).map(_.filePath).toSet
  def getNWReads(l:List[Element]): Set[NWEndpointRemote] = getNWReadsActivities(l).map(_.neRemote).toSet
  def getNWWrites(l:List[Element]): Set[NWEndpointRemote] = getNWWritesActivities(l).map(_.neRemote).toSet
  def getProcessForks(l:List[Element]): Set[ProcessPath] = getProcessForkActivities(l).map(_.subject2.processPath).toSet

  /*
  def FRA(f:FilePath) = (i:ProcessFileActivity) => i.filePath == f && i.event == EVENT_READ
  def sortedFileReads(l:List[Element]) = {
    val fr = getFilesRead(l)
    val fr_counts = fr.map(f=>(f, countActivities(l, (i:ProcessFileActivity) => i.filePath == f && i.event == EVENT_READ))).toList
    val sorted_frs = fr_counts.sortBy(_._2)
    sorted_frs
  }
*/
  def getNumActivities[A<:Element, B, C<:A:ClassTag](l:List[A], s:Set[B], f:(C, B)=>Boolean): List[(B, Int)] = {
    s.map(file=>(file, countActivities(l, (a:C) => f(a, file)))).toList.sortBy(_._2)
  }

  def sortByTime(l:List[ProcessActivity]): List[ProcessActivity] = {
    l.sortBy(_.earliestTimestampNanos.t)
  }

  def prettyPrintSorted[A](s:(A, Int)): String = s"\n\t${s._1}(${s._2})".replace(",","")

//  def getAllEventsBetween(paList:List[ProcessActivity], track:Int=0, acc:List[ProcessActivity]=List.empty):List[ProcessActivity] = paList match {
//    case (head:ProcessNWActivity)::tail if track==0 && head.event == EVENT_RECVFROM => {getAllEventsBetween(tail, 1, head::acc)}
//    case (head:ProcessNWActivity)::tail if track>0 && head.event == EVENT_RECVFROM => {getAllEventsBetween(tail, true, head::acc.tail)}
//
//    case (head:ProcessFileActivity)::tail if track && head.event == EVENT_WRITE => getAllEventsBetween(tail, false, head::acc)
//
//    case head::tail if track => getAllEventsBetween(tail, false, acc.tail)
//    case head::tail if !track => getAllEventsBetween(tail, false, acc)
//
//    case Nil => {if(acc.size%2 == 0) acc else acc.tail}.reverse
//  }

  def collectConsecutive_(paList:List[ProcessActivity], track:Boolean=false, acc:List[ProcessActivity]=List.empty):List[ProcessActivity] = paList match {
    case (head:ProcessNWActivity)::tail if !track && head.event == EVENT_RECVFROM => {collectConsecutive_(tail, true, head::acc)}
    case (head:ProcessNWActivity)::tail if track && head.event == EVENT_RECVFROM => {collectConsecutive_(tail, true, head::acc.tail)}

    case (head:ProcessFileActivity)::tail if track && head.event == EVENT_WRITE => collectConsecutive_(tail, false, head::acc)

    case head::tail if track => collectConsecutive_(tail, false, acc.tail)
    case head::tail if !track => collectConsecutive_(tail, false, acc)

    case Nil => {if(acc.size%2 == 0) acc else acc.tail}.reverse
  }
  def collectConsecutive[A1<:Element,A2<:Element](paList:List[ProcessActivity], f1:A1 => Boolean, f2:A2 => Boolean, track:Boolean=false, acc:List[ProcessActivity]=List.empty)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]):List[ProcessActivity] = paList match {
    case (head:A1)::tail if !track && f1(head) => {collectConsecutive(tail,f1,f2, true, head::acc)}
    case (head:A1)::tail if track && f1(head) => {collectConsecutive(tail,f1,f2, true, head::acc.tail)}

    case (head:A2)::tail if track && f2(head) => collectConsecutive(tail,f1,f2, false, head::acc)

    case _::tail if track => collectConsecutive(tail,f1,f2, false, acc.tail)
    case _::tail if !track => collectConsecutive(tail,f1,f2,false, acc)

    case Nil => {if(acc.size%2 == 0) acc else acc.tail}.reverse
  }


  def listOfPairs[A](l:List[A]) = l.sliding(2,2).map(x => (x.head, x.tail.head)).toList

//  def getAllActivitiesBetween = ???

  def getDirReads = ???
  def get = ???

//Files Read (# of times): ${sortedFileReads(l).map(i=>s"\n\t${i._1.path}(${i._2})").toString.replace(",","")}\n
def readableSummary(l:List[ProcessActivity]): String = {
  if (l.isEmpty) "" else{
    val p = l.head.subject.processPath
    s"""
       |==================================================
       |======= Process Path: ${p.path}
       |==================================================
       |Number of File Activities: ${countFileActivities(l)} (Reads: ${countFileReads(l)}, Writes: ${countFileWrites(l)})
       |Number of NW Activities: ${countNWActivities(l)}
       |Number of Process Activities: ${countProcessActivities(l)}
       |Number of UUIDS: ${countUniqueUuids(l)}\n
       |UUIDS (# of activities): ${getNumActivities(l,getUuids(l), (a:ProcessActivity, f:UUID)=>a.subject.uuid == f).map(prettyPrintSorted)}\n
       |Files Read (# of activities): ${getNumActivities(l,getFilesRead(l), (a:ProcessFileActivity, f:FilePath)=>a.filePath == f && isFileRead(a)).map(prettyPrintSorted)}\n
       |Files Written (# of activities): ${getNumActivities(l, getFilesWritten(l), (a: ProcessFileActivity, f: FilePath) => a.filePath == f && isFileWrite(a)).map(prettyPrintSorted)}\n
       |NW Reads (# of activities): ${getNumActivities(l, getNWReads(l), (a: ProcessNWActivity, nr: NWEndpointRemote) => a.neRemote == nr && isNWRead(a)).map(prettyPrintSorted)}\n
       |NW Writes (# of activities): ${getNumActivities(l, getNWWrites(l), (a: ProcessNWActivity, nr: NWEndpointRemote) => a.neRemote == nr && isNWWrite(a)).map(prettyPrintSorted)}\n
       |Process Forked (# of activities): ${getNumActivities(l, getProcessForks(l), (a: ProcessProcessActivity, p: ProcessPath) => a.subject2.processPath == p && isProcessFork(a)).map(prettyPrintSorted)}\n

       |File Reads followed by File Writes (downloads?): ${listOfPairs(collectConsecutive(l,isFileRead,isNWWrite)).map { case (a1, a2) => s"\n ${a1.toStr} => ${a2.toStr}"; case _ => "" }}
       |
    """.stripMargin
  }
}

  def f: (ProcessNWActivity, ProcessNWActivity) => Boolean = ProcessNWReadAndNWWrite.isApplicable
  def isEventAccept(a: ProcessActivity): Boolean = a.event == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivity): Boolean = a.event == EVENT_OPEN
  def isEventOther(a: ProcessActivity): Boolean = a.event == EVENT_OTHER
  def isEventClose(a: ProcessActivity): Boolean = a.event == EVENT_CLOSE
  def isEventExit(a: ProcessActivity): Boolean = a.event == EVENT_EXIT
  def isEventLseek(a: ProcessActivity): Boolean = a.event == EVENT_LSEEK
  def isEventReadOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP


  def compress(l:List[ProcessActivity]): List[Element] = {
    l
      .filter(i => !(isEventOpen(i) || isEventAccept(i) || isEventOther(i) || isEventClose(i) || isEventExit(i) || isEventLseek(i)))
      //merge same Consecutive Activities
      .foldRight(List.empty[Element])(genericMerge(_ == _, (a1,a2)=>a1, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(NWReadFileWrite.isApplicable, _, _), NWReadFileWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(ProcessNWReadAndNWWrite.isApplicable, _, _), ProcessNWReadAndNWWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeFileReads,FileRead.fromA1A2, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeSimilarProcessNWReadAndNWWrite, (x1, x2)=>x1, _, _))
  }

  def mergeFileReads(a1:Element, a2:Element): Boolean = {
    def check(pfa1:ProcessFileActivity, pfa2:ProcessFileActivity) = pfa1.subject == pfa2.subject && isEventReadOrMmap(pfa1.event)&& isEventReadOrMmap(pfa2.event)
    (a1, a2) match {
      case (a1: ProcessFileActivity, a2: ProcessFileActivity) if check(a1, a2) => true
      case default => false
    }
  }

  def mergeSimilarProcessNWReadAndNWWrite(a1:Element, a2:Element): Boolean = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWrite, a2: ProcessNWReadAndNWWrite) if a1 similar a2 => true
      case default => false
    }
  }

  def checkTypesAndCond[A1<:Element,A2<:Element](cond: (A1, A2)=>Boolean, a1:Element, a2:Element)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]): Boolean = (a1,a2) match{
    case (a1: A1, a2:A2) if cond(a1, a2) => true
    case _ => false
  }

  def genericMerge[A<:Element](mergeRule: (Element, Element) => Boolean, transformationRule: (Element, Element) =>A, a1:Element, paList:List[Element]): List[Element] ={
    paList match{
      case a2::tail => if (mergeRule(a1, a2)) transformationRule(a1, a2)::tail else a1::paList
      case Nil => a1::paList
    }
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivity, a2:ProcessActivity): Option[NWReadFileWrite] = {
    def check(a1:ProcessActivity, a2:ProcessActivity) = a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) if check(a1, a2) => Some(NWReadFileWrite(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivity, ProcessActivity) => Option[Element], a1:ProcessActivity, paList:List[Element]): List[Element] = {
    paList match
    {
      case (a2: ProcessActivity):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
      }
      case (a2:Element)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivity, ProcessActivity) => Boolean = _==_, a:ProcessActivity, paList:List[ProcessActivity]): List[ProcessActivity] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def applyUnaryRule(unaryRule: ProcessActivity => ProcessActivity)(a: ProcessActivity) = {
    unaryRule(a)
  }


  def getTimestamp(a: Element): Long = a match {
    case a: ProcessFileActivity => a.earliestTimestampNanos.t
    case a: ProcessProcessActivity => a.earliestTimestampNanos.t
    case a: ProcessNWActivity => a.earliestTimestampNanos.t
    case a: ProcessSrcSinkActivity => a.earliestTimestampNanos.t
    case default => ???
  }
}



case class FileRead(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends Element{
}

object FileRead {
  def fromA1A2(a1:Element, a2:Element): FileRead = (a1, a2) match {
    case (a1: ProcessFileActivity, a2: ProcessFileActivity) =>
      new FileRead(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


//trait ProcessActivitiesSetOfUptoTwo extends Element



/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWrite(pna1: ProcessNWActivity, pna2: ProcessNWActivity) extends Element{

  def similar (a:ProcessNWReadAndNWWrite): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWrite(" +
      pna1.subject.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }

}

object ProcessNWReadAndNWWrite {
  def apply(pna1: Element, pna2: Element): ProcessNWReadAndNWWrite = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivity, pna2:ProcessNWActivity) => new ProcessNWReadAndNWWrite(pna1, pna2)
      case default => ???
    }
  }
  def isApplicable(a1: ProcessNWActivity, a2: ProcessNWActivity): Boolean =
    a1.subject == a2.subject && // same process
      a1.neLocal == a2.neLocal && a1.neRemote == a2.neRemote && // same n/w endpoint
      a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE // Read followed by a write

}

case class NWReadFileWrite(pna1: ProcessNWActivity, pfa2: ProcessFileActivity) extends Element

object NWReadFileWrite{
  def apply(a1:Element, a2:Element): NWReadFileWrite = {
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) => new NWReadFileWrite(a1, a2)
      case default => ???
    }
  }

  def isApplicable(pna1:ProcessNWActivity, pfa2:ProcessFileActivity) =
    pna1.event == EVENT_RECVFROM && pfa2.event == EVENT_WRITE && pna1.subject == pfa2.subject

}


case class ProcessActivityList(activities: List[ProcessActivity]) extends Element




/*
package com.galois.adapt

import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_LSEEK, EVENT_MMAP, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}
import scala.reflect.ClassTag

trait Element
trait ComposedActivity

object SummaryParser {

  def f: (ProcessNWActivity, ProcessNWActivity) => Boolean = ProcessNWReadAndNWWrite.isApplicable

  def isEventAccept(a: ProcessActivity): Boolean = a.event == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivity): Boolean = a.event == EVENT_OPEN
  def isEventOther(a: ProcessActivity): Boolean = a.event == EVENT_OTHER
  def isEventClose(a: ProcessActivity): Boolean = a.event == EVENT_CLOSE
  def isEventExit(a: ProcessActivity): Boolean = a.event == EVENT_EXIT
  def isEventLseek(a: ProcessActivity): Boolean = a.event == EVENT_LSEEK
  def isEventReadOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP


  def compress(l:List[ProcessActivity]): List[Element] = {
    l
      .filter(i => !(isEventOpen(i) || isEventAccept(i) || isEventOther(i) || isEventClose(i) || isEventExit(i) || isEventLseek(i)))
      //merge same Consecutive Activities
      .foldRight(List.empty[Element])(genericMerge(_ == _, (a1,a2)=>a1, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(NWReadFileWrite.isApplicable, _, _), NWReadFileWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(ProcessNWReadAndNWWrite.isApplicable, _, _), ProcessNWReadAndNWWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeFileReads,FileRead.fromA1A2, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeSimilarProcessNWReadAndNWWrite, (x1, x2)=>x1, _, _))
  }

  def mergeFileReads(a1:Element, a2:Element): Boolean = {
    def check(pfa1:ProcessFileActivity, pfa2:ProcessFileActivity) = pfa1.subject == pfa2.subject && isEventReadOrMmap(pfa1.event)&& isEventReadOrMmap(pfa2.event)
    (a1, a2) match {
      case (a1: ProcessFileActivity, a2: ProcessFileActivity) if check(a1, a2) => true
      case default => false
    }
  }

  def mergeSimilarProcessNWReadAndNWWrite(a1:Element, a2:Element): Boolean = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWrite, a2: ProcessNWReadAndNWWrite) if a1 similar a2 => true
      case default => false
    }
  }

  def checkTypesAndCond[A1<:Element,A2<:Element](cond: (A1, A2)=>Boolean, a1:Element, a2:Element)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]): Boolean = (a1,a2) match{
    case (a1: A1, a2:A2) if cond(a1, a2) => true
    case _ => false
  }

  def genericMerge[A<:Element](mergeRule: (Element, Element) => Boolean, transformationRule: (Element, Element) =>A, a1:Element, paList:List[Element]): List[Element] ={
    paList match{
      case a2::tail => if (mergeRule(a1, a2)) transformationRule(a1, a2)::tail else a1::paList
      case Nil => a1::paList
    }
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivity, a2:ProcessActivity): Option[NWReadFileWrite] = {
    def check(a1:ProcessActivity, a2:ProcessActivity) = a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) if check(a1, a2) => Some(NWReadFileWrite(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivity, ProcessActivity) => Option[Element], a1:ProcessActivity, paList:List[Element]): List[Element] = {
    paList match
    {
      case (a2: ProcessActivity):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
      }
      case (a2:Element)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivity, ProcessActivity) => Boolean = _==_, a:ProcessActivity, paList:List[ProcessActivity]): List[ProcessActivity] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def applyUnaryRule(unaryRule: ProcessActivity => ProcessActivity)(a: ProcessActivity) = {
    unaryRule(a)
  }


  def getTimestamp(a: Element): TimestampNanos = a match {
    case a: ProcessFileActivity => a.earliestTimestampNanos
    case a: ProcessProcessActivity => a.earliestTimestampNanos
    case a: ProcessNWActivity => a.earliestTimestampNanos
    case a: ProcessSrcSinkActivity => a.earliestTimestampNanos
    case default => ???
  }
}



case class FileRead(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends Element{
}

object FileRead {
  def fromA1A2(a1:Element, a2:Element): FileRead = (a1, a2) match {
    case (a1: ProcessFileActivity, a2: ProcessFileActivity) =>
      new FileRead(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


//trait ProcessActivitiesSetOfUptoTwo extends Element



/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWrite(pna1: ProcessNWActivity, pna2: ProcessNWActivity) extends Element{

  def similar (a:ProcessNWReadAndNWWrite): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWrite(" +
      pna1.subject.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }

}

object ProcessNWReadAndNWWrite {
  def apply(pna1: Element, pna2: Element): ProcessNWReadAndNWWrite = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivity, pna2:ProcessNWActivity) => new ProcessNWReadAndNWWrite(pna1, pna2)
      case default => ???
    }
  }
  def isApplicable(a1: ProcessNWActivity, a2: ProcessNWActivity): Boolean =
    a1.subject == a2.subject && // same process
      a1.neLocal == a2.neLocal && a1.neRemote == a2.neRemote && // same n/w endpoint
      a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE // Read followed by a write

}

case class NWReadFileWrite(pna1: ProcessNWActivity, pfa2: ProcessFileActivity) extends Element

object NWReadFileWrite{
  def apply(a1:Element, a2:Element): NWReadFileWrite = {
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) => new NWReadFileWrite(a1, a2)
      case default => ???
    }
  }

  def isApplicable(pna1:ProcessNWActivity, pfa2:ProcessFileActivity) =
    pna1.event == EVENT_RECVFROM && pfa2.event == EVENT_WRITE && pna1.subject == pfa2.subject

}


case class ProcessActivityList(activities: List[ProcessActivity]) extends Element
*/