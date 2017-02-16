/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm14;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * A provenance tag defines source dependence on specific data sources (inputs).
     * A tag identifier is typically bound to a source and used by the tracking system to
     * capture dependence on this source input.
     *
     * ProvenanceTagNode defines one step of provenance for a value
     * (i.e., one read from a source or write to a sink), a reference
     * to the previous provenance of the value (if any), and the tag
     * operation that resulted the tagId of this ProvenanceTagNode */
@org.apache.avro.specific.AvroGenerated
public class ProvenanceTagNode extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 6464402056878438564L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ProvenanceTagNode\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* A provenance tag defines source dependence on specific data sources (inputs).\\n     * A tag identifier is typically bound to a source and used by the tracking system to\\n     * capture dependence on this source input.\\n     *\\n     * ProvenanceTagNode defines one step of provenance for a value\\n     * (i.e., one read from a source or write to a sink), a reference\\n     * to the previous provenance of the value (if any), and the tag\\n     * operation that resulted the tagId of this ProvenanceTagNode\",\"fields\":[{\"name\":\"tagId\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Tag ID for this node *\"},{\"name\":\"programPoint\",\"type\":[\"null\",\"string\"],\"doc\":\"The program point where the event was triggered (e.g., executable and line number), (Optional)\",\"default\":null},{\"name\":\"prevTagId\",\"type\":[\"null\",\"UUID\"],\"doc\":\"The previous tag for this value *\",\"default\":null},{\"name\":\"opcode\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"TagOpCode\",\"doc\":\"* The tag opcode describes the provenance relation i.e., how multiple sources are combined to\\n     * produce the output. We identify the following provenance relations\\n     *\\n     *   TAG_OP_UNION,         the output is the union of its inputs\\n     *   TAG_OP_ENCODE         the output is some encoding of the input\\n     *   TAG_OP_STRONG         this is more qualitative (coarse) saying there is strong dependence\\n     *   TAG_OP_MEDIUM         this is more qualitative (coarse) saying there is medium dependence\\n     *   TAG_OP_WEAK           this is more qualitative (coarse) saying there is weak   dependence\",\"symbols\":[\"TAG_OP_UNION\",\"TAG_OP_ENCODE\",\"TAG_OP_STRONG\",\"TAG_OP_MEDIUM\",\"TAG_OP_WEAK\"]}],\"doc\":\"Tag operation that resulted in the tagId of this ProvenanceTagNode *\",\"default\":null},{\"name\":\"tagIds\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"UUID\"}],\"default\":null},{\"name\":\"itag\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"IntegrityTag\",\"doc\":\"* The integrity tag may be used to specify the initial integrity of an entity,\\n     * or to endorse its content after performing appropriate checking/sanitization.\",\"symbols\":[\"INTEGRITY_UNTRUSTED\",\"INTEGRITY_BENIGN\",\"INTEGRITY_INVULNERABLE\"]}],\"doc\":\"The integrity tag may be used to specify the intial\\n         *  integrity of an entity, or to endorse it content after\\n         *  performing appropriate checking/sanitization.\",\"default\":null},{\"name\":\"ctag\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"ConfidentialityTag\",\"doc\":\"* The confidentiality tag may be used to specify the initial confidentiality of an entity,\\n     * or to declassify its content after performing appropriate checking/sanitization.\",\"symbols\":[\"CONFIDENTIALITY_SECRET\",\"CONFIDENTIALITY_SENSITIVE\",\"CONFIDENTIALITY_PRIVATE\",\"CONFIDENTIALITY_PUBLIC\"]}],\"doc\":\"* The confidentiality tag may be used to specify the initial\\n         * confidentiality of an entity, or to declassify its content\\n         * after performing appropriate checking/sanitization.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Tag ID for this node * */
  @Deprecated public UUID tagId;
  /** The program point where the event was triggered (e.g., executable and line number), (Optional) */
  @Deprecated public java.lang.CharSequence programPoint;
  /** The previous tag for this value * */
  @Deprecated public UUID prevTagId;
  /** Tag operation that resulted in the tagId of this ProvenanceTagNode * */
  @Deprecated public TagOpCode opcode;
  @Deprecated public java.util.List<UUID> tagIds;
  /** The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization. */
  @Deprecated public IntegrityTag itag;
  /** * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization. */
  @Deprecated public ConfidentialityTag ctag;
  /** * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version. */
  @Deprecated public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> properties;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public ProvenanceTagNode() {}

  /**
   * All-args constructor.
   * @param tagId Tag ID for this node *
   * @param programPoint The program point where the event was triggered (e.g., executable and line number), (Optional)
   * @param prevTagId The previous tag for this value *
   * @param opcode Tag operation that resulted in the tagId of this ProvenanceTagNode *
   * @param tagIds The new value for tagIds
   * @param itag The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
   * @param ctag * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
   * @param properties * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
   */
  public ProvenanceTagNode(UUID tagId, java.lang.CharSequence programPoint, UUID prevTagId, TagOpCode opcode, java.util.List<UUID> tagIds, IntegrityTag itag, ConfidentialityTag ctag, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> properties) {
    this.tagId = tagId;
    this.programPoint = programPoint;
    this.prevTagId = prevTagId;
    this.opcode = opcode;
    this.tagIds = tagIds;
    this.itag = itag;
    this.ctag = ctag;
    this.properties = properties;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return tagId;
    case 1: return programPoint;
    case 2: return prevTagId;
    case 3: return opcode;
    case 4: return tagIds;
    case 5: return itag;
    case 6: return ctag;
    case 7: return properties;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: tagId = (UUID)value$; break;
    case 1: programPoint = (java.lang.CharSequence)value$; break;
    case 2: prevTagId = (UUID)value$; break;
    case 3: opcode = (TagOpCode)value$; break;
    case 4: tagIds = (java.util.List<UUID>)value$; break;
    case 5: itag = (IntegrityTag)value$; break;
    case 6: ctag = (ConfidentialityTag)value$; break;
    case 7: properties = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'tagId' field.
   * @return Tag ID for this node *
   */
  public UUID getTagId() {
    return tagId;
  }

  /**
   * Sets the value of the 'tagId' field.
   * Tag ID for this node *
   * @param value the value to set.
   */
  public void setTagId(UUID value) {
    this.tagId = value;
  }

  /**
   * Gets the value of the 'programPoint' field.
   * @return The program point where the event was triggered (e.g., executable and line number), (Optional)
   */
  public java.lang.CharSequence getProgramPoint() {
    return programPoint;
  }

  /**
   * Sets the value of the 'programPoint' field.
   * The program point where the event was triggered (e.g., executable and line number), (Optional)
   * @param value the value to set.
   */
  public void setProgramPoint(java.lang.CharSequence value) {
    this.programPoint = value;
  }

  /**
   * Gets the value of the 'prevTagId' field.
   * @return The previous tag for this value *
   */
  public UUID getPrevTagId() {
    return prevTagId;
  }

  /**
   * Sets the value of the 'prevTagId' field.
   * The previous tag for this value *
   * @param value the value to set.
   */
  public void setPrevTagId(UUID value) {
    this.prevTagId = value;
  }

  /**
   * Gets the value of the 'opcode' field.
   * @return Tag operation that resulted in the tagId of this ProvenanceTagNode *
   */
  public TagOpCode getOpcode() {
    return opcode;
  }

  /**
   * Sets the value of the 'opcode' field.
   * Tag operation that resulted in the tagId of this ProvenanceTagNode *
   * @param value the value to set.
   */
  public void setOpcode(TagOpCode value) {
    this.opcode = value;
  }

  /**
   * Gets the value of the 'tagIds' field.
   * @return The value of the 'tagIds' field.
   */
  public java.util.List<UUID> getTagIds() {
    return tagIds;
  }

  /**
   * Sets the value of the 'tagIds' field.
   * @param value the value to set.
   */
  public void setTagIds(java.util.List<UUID> value) {
    this.tagIds = value;
  }

  /**
   * Gets the value of the 'itag' field.
   * @return The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
   */
  public IntegrityTag getItag() {
    return itag;
  }

  /**
   * Sets the value of the 'itag' field.
   * The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
   * @param value the value to set.
   */
  public void setItag(IntegrityTag value) {
    this.itag = value;
  }

  /**
   * Gets the value of the 'ctag' field.
   * @return * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
   */
  public ConfidentialityTag getCtag() {
    return ctag;
  }

  /**
   * Sets the value of the 'ctag' field.
   * * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
   * @param value the value to set.
   */
  public void setCtag(ConfidentialityTag value) {
    this.ctag = value;
  }

  /**
   * Gets the value of the 'properties' field.
   * @return * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getProperties() {
    return properties;
  }

  /**
   * Sets the value of the 'properties' field.
   * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
   * @param value the value to set.
   */
  public void setProperties(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.properties = value;
  }

  /**
   * Creates a new ProvenanceTagNode RecordBuilder.
   * @return A new ProvenanceTagNode RecordBuilder
   */
  public static ProvenanceTagNode.Builder newBuilder() {
    return new ProvenanceTagNode.Builder();
  }

  /**
   * Creates a new ProvenanceTagNode RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new ProvenanceTagNode RecordBuilder
   */
  public static ProvenanceTagNode.Builder newBuilder(ProvenanceTagNode.Builder other) {
    return new ProvenanceTagNode.Builder(other);
  }

  /**
   * Creates a new ProvenanceTagNode RecordBuilder by copying an existing ProvenanceTagNode instance.
   * @param other The existing instance to copy.
   * @return A new ProvenanceTagNode RecordBuilder
   */
  public static ProvenanceTagNode.Builder newBuilder(ProvenanceTagNode other) {
    return new ProvenanceTagNode.Builder(other);
  }

  /**
   * RecordBuilder for ProvenanceTagNode instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ProvenanceTagNode>
    implements org.apache.avro.data.RecordBuilder<ProvenanceTagNode> {

    /** Tag ID for this node * */
    private UUID tagId;
    /** The program point where the event was triggered (e.g., executable and line number), (Optional) */
    private java.lang.CharSequence programPoint;
    /** The previous tag for this value * */
    private UUID prevTagId;
    /** Tag operation that resulted in the tagId of this ProvenanceTagNode * */
    private TagOpCode opcode;
    private java.util.List<UUID> tagIds;
    /** The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization. */
    private IntegrityTag itag;
    /** * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization. */
    private ConfidentialityTag ctag;
    /** * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version. */
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> properties;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(ProvenanceTagNode.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.tagId)) {
        this.tagId = data().deepCopy(fields()[0].schema(), other.tagId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.programPoint)) {
        this.programPoint = data().deepCopy(fields()[1].schema(), other.programPoint);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.prevTagId)) {
        this.prevTagId = data().deepCopy(fields()[2].schema(), other.prevTagId);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.opcode)) {
        this.opcode = data().deepCopy(fields()[3].schema(), other.opcode);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.tagIds)) {
        this.tagIds = data().deepCopy(fields()[4].schema(), other.tagIds);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.itag)) {
        this.itag = data().deepCopy(fields()[5].schema(), other.itag);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.ctag)) {
        this.ctag = data().deepCopy(fields()[6].schema(), other.ctag);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.properties)) {
        this.properties = data().deepCopy(fields()[7].schema(), other.properties);
        fieldSetFlags()[7] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing ProvenanceTagNode instance
     * @param other The existing instance to copy.
     */
    private Builder(ProvenanceTagNode other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.tagId)) {
        this.tagId = data().deepCopy(fields()[0].schema(), other.tagId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.programPoint)) {
        this.programPoint = data().deepCopy(fields()[1].schema(), other.programPoint);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.prevTagId)) {
        this.prevTagId = data().deepCopy(fields()[2].schema(), other.prevTagId);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.opcode)) {
        this.opcode = data().deepCopy(fields()[3].schema(), other.opcode);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.tagIds)) {
        this.tagIds = data().deepCopy(fields()[4].schema(), other.tagIds);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.itag)) {
        this.itag = data().deepCopy(fields()[5].schema(), other.itag);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.ctag)) {
        this.ctag = data().deepCopy(fields()[6].schema(), other.ctag);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.properties)) {
        this.properties = data().deepCopy(fields()[7].schema(), other.properties);
        fieldSetFlags()[7] = true;
      }
    }

    /**
      * Gets the value of the 'tagId' field.
      * Tag ID for this node *
      * @return The value.
      */
    public UUID getTagId() {
      return tagId;
    }

    /**
      * Sets the value of the 'tagId' field.
      * Tag ID for this node *
      * @param value The value of 'tagId'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setTagId(UUID value) {
      validate(fields()[0], value);
      this.tagId = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'tagId' field has been set.
      * Tag ID for this node *
      * @return True if the 'tagId' field has been set, false otherwise.
      */
    public boolean hasTagId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'tagId' field.
      * Tag ID for this node *
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearTagId() {
      tagId = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'programPoint' field.
      * The program point where the event was triggered (e.g., executable and line number), (Optional)
      * @return The value.
      */
    public java.lang.CharSequence getProgramPoint() {
      return programPoint;
    }

    /**
      * Sets the value of the 'programPoint' field.
      * The program point where the event was triggered (e.g., executable and line number), (Optional)
      * @param value The value of 'programPoint'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setProgramPoint(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.programPoint = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'programPoint' field has been set.
      * The program point where the event was triggered (e.g., executable and line number), (Optional)
      * @return True if the 'programPoint' field has been set, false otherwise.
      */
    public boolean hasProgramPoint() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'programPoint' field.
      * The program point where the event was triggered (e.g., executable and line number), (Optional)
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearProgramPoint() {
      programPoint = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'prevTagId' field.
      * The previous tag for this value *
      * @return The value.
      */
    public UUID getPrevTagId() {
      return prevTagId;
    }

    /**
      * Sets the value of the 'prevTagId' field.
      * The previous tag for this value *
      * @param value The value of 'prevTagId'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setPrevTagId(UUID value) {
      validate(fields()[2], value);
      this.prevTagId = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'prevTagId' field has been set.
      * The previous tag for this value *
      * @return True if the 'prevTagId' field has been set, false otherwise.
      */
    public boolean hasPrevTagId() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'prevTagId' field.
      * The previous tag for this value *
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearPrevTagId() {
      prevTagId = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'opcode' field.
      * Tag operation that resulted in the tagId of this ProvenanceTagNode *
      * @return The value.
      */
    public TagOpCode getOpcode() {
      return opcode;
    }

    /**
      * Sets the value of the 'opcode' field.
      * Tag operation that resulted in the tagId of this ProvenanceTagNode *
      * @param value The value of 'opcode'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setOpcode(TagOpCode value) {
      validate(fields()[3], value);
      this.opcode = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'opcode' field has been set.
      * Tag operation that resulted in the tagId of this ProvenanceTagNode *
      * @return True if the 'opcode' field has been set, false otherwise.
      */
    public boolean hasOpcode() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'opcode' field.
      * Tag operation that resulted in the tagId of this ProvenanceTagNode *
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearOpcode() {
      opcode = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'tagIds' field.
      * @return The value.
      */
    public java.util.List<UUID> getTagIds() {
      return tagIds;
    }

    /**
      * Sets the value of the 'tagIds' field.
      * @param value The value of 'tagIds'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setTagIds(java.util.List<UUID> value) {
      validate(fields()[4], value);
      this.tagIds = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'tagIds' field has been set.
      * @return True if the 'tagIds' field has been set, false otherwise.
      */
    public boolean hasTagIds() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'tagIds' field.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearTagIds() {
      tagIds = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'itag' field.
      * The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
      * @return The value.
      */
    public IntegrityTag getItag() {
      return itag;
    }

    /**
      * Sets the value of the 'itag' field.
      * The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
      * @param value The value of 'itag'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setItag(IntegrityTag value) {
      validate(fields()[5], value);
      this.itag = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'itag' field has been set.
      * The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
      * @return True if the 'itag' field has been set, false otherwise.
      */
    public boolean hasItag() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'itag' field.
      * The integrity tag may be used to specify the intial
         *  integrity of an entity, or to endorse it content after
         *  performing appropriate checking/sanitization.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearItag() {
      itag = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'ctag' field.
      * * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
      * @return The value.
      */
    public ConfidentialityTag getCtag() {
      return ctag;
    }

    /**
      * Sets the value of the 'ctag' field.
      * * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
      * @param value The value of 'ctag'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setCtag(ConfidentialityTag value) {
      validate(fields()[6], value);
      this.ctag = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'ctag' field has been set.
      * * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
      * @return True if the 'ctag' field has been set, false otherwise.
      */
    public boolean hasCtag() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'ctag' field.
      * * The confidentiality tag may be used to specify the initial
         * confidentiality of an entity, or to declassify its content
         * after performing appropriate checking/sanitization.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearCtag() {
      ctag = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /**
      * Gets the value of the 'properties' field.
      * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
      * @return The value.
      */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getProperties() {
      return properties;
    }

    /**
      * Sets the value of the 'properties' field.
      * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
      * @param value The value of 'properties'.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder setProperties(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[7], value);
      this.properties = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /**
      * Checks whether the 'properties' field has been set.
      * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
      * @return True if the 'properties' field has been set, false otherwise.
      */
    public boolean hasProperties() {
      return fieldSetFlags()[7];
    }


    /**
      * Clears the value of the 'properties' field.
      * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
      * @return This builder.
      */
    public ProvenanceTagNode.Builder clearProperties() {
      properties = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    @Override
    public ProvenanceTagNode build() {
      try {
        ProvenanceTagNode record = new ProvenanceTagNode();
        record.tagId = fieldSetFlags()[0] ? this.tagId : (UUID) defaultValue(fields()[0]);
        record.programPoint = fieldSetFlags()[1] ? this.programPoint : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.prevTagId = fieldSetFlags()[2] ? this.prevTagId : (UUID) defaultValue(fields()[2]);
        record.opcode = fieldSetFlags()[3] ? this.opcode : (TagOpCode) defaultValue(fields()[3]);
        record.tagIds = fieldSetFlags()[4] ? this.tagIds : (java.util.List<UUID>) defaultValue(fields()[4]);
        record.itag = fieldSetFlags()[5] ? this.itag : (IntegrityTag) defaultValue(fields()[5]);
        record.ctag = fieldSetFlags()[6] ? this.ctag : (ConfidentialityTag) defaultValue(fields()[6]);
        record.properties = fieldSetFlags()[7] ? this.properties : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) defaultValue(fields()[7]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}