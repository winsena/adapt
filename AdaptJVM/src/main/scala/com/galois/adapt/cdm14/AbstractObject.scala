package com.galois.adapt.cdm14
import com.bbn.tc.schema.avro.cdm14
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class AbstractObject(
                           source: InstrumentationSource,
                           permission: Option[FixedShort] = None,  // fixed size = 2
                           epoch: Option[Int] = None,
                           properties: Option[Map[String,String]] = None
                         ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    //    label, "AbstractObject",
    "source", source.toString
  ) ++
    permission.fold[List[Any]](List.empty)(v => List("permission", v.bytes.toString)) ++
    epoch.fold[List[Any]](List.empty)(v => List("epoch", v)) ++
    DBOpt.fromKeyValMap(properties)
}

case object AbstractObject extends CDM14Constructor[AbstractObject] {
  type RawCDMType = cdm14.AbstractObject

  def from(cdm: RawCDM14Type): Try[AbstractObject] = Try {
    AbstractObject(
      cdm.getSource,
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}