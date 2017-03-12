package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class SrcSinkObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends CDM15 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "SrcSinkObject",
    "uuid", uuid,
    "srcSinkType", srcSinkType.toString
  ) ++
    baseObject.asDBKeyValues ++
    fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v))

  def asDBEdges = Nil

  def getUuid = uuid
}

case object SrcSinkObject extends CDM15Constructor[SrcSinkObject] {
  type RawCDMType = cdm15.SrcSinkObject

  def from(cdm: RawCDM15Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
