/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm13;
@SuppressWarnings("all")
/** * The type of principal may be local to the host, or remote users/systems. */
@org.apache.avro.specific.AvroGenerated
public enum PrincipalType {
  PRINCIPAL_LOCAL, PRINCIPAL_REMOTE  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"PrincipalType\",\"namespace\":\"com.bbn.tc.schema.avro.cdm13\",\"doc\":\"* The type of principal may be local to the host, or remote users/systems.\",\"symbols\":[\"PRINCIPAL_LOCAL\",\"PRINCIPAL_REMOTE\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
