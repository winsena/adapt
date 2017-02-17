/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm13;
@SuppressWarnings("all")
/** * The tag opcode describes the provenance relation i.e., how multiple sources are combined to
     * produce the output. We identify the following provenance relations
     *
     *   TAG_OP_SEQUENCE       the output is derived from the specified inputs in order, sequentially
     *TAG_OP_UNION,         the output is the union of its inputs
     * TAG_OP_ENCODE         the output is some encoding of the input
     * TAG_OP_STRONG         this is more qualitative (coarse) saying there is strong dependence
     *   TAG_OP_MEDIUM         this is more qualitative (coarse) saying there is medium dependence
     *   TAG_OP_WEAK           this is more qualitative (coarse) saying there is weak   dependence */
@org.apache.avro.specific.AvroGenerated
public enum TagOpCode {
  TAG_OP_SEQUENCE, TAG_OP_UNION, TAG_OP_ENCODE, TAG_OP_STRONG, TAG_OP_MEDIUM, TAG_OP_WEAK  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"TagOpCode\",\"namespace\":\"com.bbn.tc.schema.avro.cdm13\",\"doc\":\"* The tag opcode describes the provenance relation i.e., how multiple sources are combined to\\n     * produce the output. We identify the following provenance relations\\n     *\\n     *   TAG_OP_SEQUENCE       the output is derived from the specified inputs in order, sequentially\\n     *TAG_OP_UNION,         the output is the union of its inputs\\n     * TAG_OP_ENCODE         the output is some encoding of the input\\n     * TAG_OP_STRONG         this is more qualitative (coarse) saying there is strong dependence\\n     *   TAG_OP_MEDIUM         this is more qualitative (coarse) saying there is medium dependence\\n     *   TAG_OP_WEAK           this is more qualitative (coarse) saying there is weak   dependence\",\"symbols\":[\"TAG_OP_SEQUENCE\",\"TAG_OP_UNION\",\"TAG_OP_ENCODE\",\"TAG_OP_STRONG\",\"TAG_OP_MEDIUM\",\"TAG_OP_WEAK\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
}
