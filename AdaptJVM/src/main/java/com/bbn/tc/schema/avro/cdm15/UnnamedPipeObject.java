/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents an unnamed pipe. Instantiates an AbstractObject. */
@org.apache.avro.specific.AvroGenerated
public class UnnamedPipeObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -1596483736832640508L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UnnamedPipeObject\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* Represents an unnamed pipe. Instantiates an AbstractObject.\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which\\n     *  could include sockets, files, memory, and any data in general\\n     *  that can be an input and/or output to an event.  This record\\n     *  is intended to be abstract i.e., one should not instantiate an\\n     *  Object but rather instantiate one of its sub types (ie,\\n     *  encapsulating records) FileObject, UnnamedPipeObject,\\n     *  RegistryKeyObject, NetFlowObject, MemoryObject, or\\n     *  SrcSinkObject.\",\"fields\":[{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"epoch\",\"type\":[\"null\",\"int\"],\"doc\":\"* Used to track when an object is deleted and a new one is\\n         * created with the same identifier. This is useful for when\\n         * UUIDs are based on something not likely to be unique, such\\n         * as file path.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null}]},\"doc\":\"The base object attributes\"},{\"name\":\"sourceFileDescriptor\",\"type\":\"int\",\"doc\":\"File descriptors for reading and writing.\"},{\"name\":\"sinkFileDescriptor\",\"type\":\"int\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.AbstractObject baseObject;
  /** File descriptors for reading and writing. */
  @Deprecated public int sourceFileDescriptor;
  @Deprecated public int sinkFileDescriptor;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public UnnamedPipeObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param sourceFileDescriptor File descriptors for reading and writing.
   * @param sinkFileDescriptor The new value for sinkFileDescriptor
   */
  public UnnamedPipeObject(com.bbn.tc.schema.avro.UUID uuid, com.bbn.tc.schema.avro.AbstractObject baseObject, java.lang.Integer sourceFileDescriptor, java.lang.Integer sinkFileDescriptor) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.sourceFileDescriptor = sourceFileDescriptor;
    this.sinkFileDescriptor = sinkFileDescriptor;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return sourceFileDescriptor;
    case 3: return sinkFileDescriptor;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.AbstractObject)value$; break;
    case 2: sourceFileDescriptor = (java.lang.Integer)value$; break;
    case 3: sinkFileDescriptor = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return Universally unique identifier for the object
   */
  public com.bbn.tc.schema.avro.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * Universally unique identifier for the object
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'baseObject' field.
   * @return The base object attributes
   */
  public com.bbn.tc.schema.avro.AbstractObject getBaseObject() {
    return baseObject;
  }

  /**
   * Sets the value of the 'baseObject' field.
   * The base object attributes
   * @param value the value to set.
   */
  public void setBaseObject(com.bbn.tc.schema.avro.AbstractObject value) {
    this.baseObject = value;
  }

  /**
   * Gets the value of the 'sourceFileDescriptor' field.
   * @return File descriptors for reading and writing.
   */
  public java.lang.Integer getSourceFileDescriptor() {
    return sourceFileDescriptor;
  }

  /**
   * Sets the value of the 'sourceFileDescriptor' field.
   * File descriptors for reading and writing.
   * @param value the value to set.
   */
  public void setSourceFileDescriptor(java.lang.Integer value) {
    this.sourceFileDescriptor = value;
  }

  /**
   * Gets the value of the 'sinkFileDescriptor' field.
   * @return The value of the 'sinkFileDescriptor' field.
   */
  public java.lang.Integer getSinkFileDescriptor() {
    return sinkFileDescriptor;
  }

  /**
   * Sets the value of the 'sinkFileDescriptor' field.
   * @param value the value to set.
   */
  public void setSinkFileDescriptor(java.lang.Integer value) {
    this.sinkFileDescriptor = value;
  }

  /**
   * Creates a new UnnamedPipeObject RecordBuilder.
   * @return A new UnnamedPipeObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.UnnamedPipeObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.UnnamedPipeObject.Builder();
  }

  /**
   * Creates a new UnnamedPipeObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new UnnamedPipeObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.UnnamedPipeObject.Builder newBuilder(com.bbn.tc.schema.avro.UnnamedPipeObject.Builder other) {
    return new com.bbn.tc.schema.avro.UnnamedPipeObject.Builder(other);
  }

  /**
   * Creates a new UnnamedPipeObject RecordBuilder by copying an existing UnnamedPipeObject instance.
   * @param other The existing instance to copy.
   * @return A new UnnamedPipeObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.UnnamedPipeObject.Builder newBuilder(com.bbn.tc.schema.avro.UnnamedPipeObject other) {
    return new com.bbn.tc.schema.avro.UnnamedPipeObject.Builder(other);
  }

  /**
   * RecordBuilder for UnnamedPipeObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<UnnamedPipeObject>
    implements org.apache.avro.data.RecordBuilder<UnnamedPipeObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.AbstractObject.Builder baseObjectBuilder;
    /** File descriptors for reading and writing. */
    private int sourceFileDescriptor;
    private int sinkFileDescriptor;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.UnnamedPipeObject.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.baseObject)) {
        this.baseObject = data().deepCopy(fields()[1].schema(), other.baseObject);
        fieldSetFlags()[1] = true;
      }
      if (other.hasBaseObjectBuilder()) {
        this.baseObjectBuilder = com.bbn.tc.schema.avro.AbstractObject.newBuilder(other.getBaseObjectBuilder());
      }
      if (isValidValue(fields()[2], other.sourceFileDescriptor)) {
        this.sourceFileDescriptor = data().deepCopy(fields()[2].schema(), other.sourceFileDescriptor);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.sinkFileDescriptor)) {
        this.sinkFileDescriptor = data().deepCopy(fields()[3].schema(), other.sinkFileDescriptor);
        fieldSetFlags()[3] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing UnnamedPipeObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.UnnamedPipeObject other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.baseObject)) {
        this.baseObject = data().deepCopy(fields()[1].schema(), other.baseObject);
        fieldSetFlags()[1] = true;
      }
      this.baseObjectBuilder = null;
      if (isValidValue(fields()[2], other.sourceFileDescriptor)) {
        this.sourceFileDescriptor = data().deepCopy(fields()[2].schema(), other.sourceFileDescriptor);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.sinkFileDescriptor)) {
        this.sinkFileDescriptor = data().deepCopy(fields()[3].schema(), other.sinkFileDescriptor);
        fieldSetFlags()[3] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder setUuid(com.bbn.tc.schema.avro.UUID value) {
      validate(fields()[0], value);
      this.uuid = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'uuid' field has been set.
      * Universally unique identifier for the object
      * @return True if the 'uuid' field has been set, false otherwise.
      */
    public boolean hasUuid() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'baseObject' field.
      * The base object attributes
      * @return The value.
      */
    public com.bbn.tc.schema.avro.AbstractObject getBaseObject() {
      return baseObject;
    }

    /**
      * Sets the value of the 'baseObject' field.
      * The base object attributes
      * @param value The value of 'baseObject'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder setBaseObject(com.bbn.tc.schema.avro.AbstractObject value) {
      validate(fields()[1], value);
      this.baseObjectBuilder = null;
      this.baseObject = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'baseObject' field has been set.
      * The base object attributes
      * @return True if the 'baseObject' field has been set, false otherwise.
      */
    public boolean hasBaseObject() {
      return fieldSetFlags()[1];
    }

    /**
     * Gets the Builder instance for the 'baseObject' field and creates one if it doesn't exist yet.
     * The base object attributes
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.AbstractObject.Builder getBaseObjectBuilder() {
      if (baseObjectBuilder == null) {
        if (hasBaseObject()) {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.newBuilder(baseObject));
        } else {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.newBuilder());
        }
      }
      return baseObjectBuilder;
    }

    /**
     * Sets the Builder instance for the 'baseObject' field
     * The base object attributes
     * @param value The builder instance that must be set.
     * @return This builder.
     */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.AbstractObject.Builder value) {
      clearBaseObject();
      baseObjectBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'baseObject' field has an active Builder instance
     * The base object attributes
     * @return True if the 'baseObject' field has an active Builder instance
     */
    public boolean hasBaseObjectBuilder() {
      return baseObjectBuilder != null;
    }

    /**
      * Clears the value of the 'baseObject' field.
      * The base object attributes
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'sourceFileDescriptor' field.
      * File descriptors for reading and writing.
      * @return The value.
      */
    public java.lang.Integer getSourceFileDescriptor() {
      return sourceFileDescriptor;
    }

    /**
      * Sets the value of the 'sourceFileDescriptor' field.
      * File descriptors for reading and writing.
      * @param value The value of 'sourceFileDescriptor'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder setSourceFileDescriptor(int value) {
      validate(fields()[2], value);
      this.sourceFileDescriptor = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'sourceFileDescriptor' field has been set.
      * File descriptors for reading and writing.
      * @return True if the 'sourceFileDescriptor' field has been set, false otherwise.
      */
    public boolean hasSourceFileDescriptor() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'sourceFileDescriptor' field.
      * File descriptors for reading and writing.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder clearSourceFileDescriptor() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'sinkFileDescriptor' field.
      * @return The value.
      */
    public java.lang.Integer getSinkFileDescriptor() {
      return sinkFileDescriptor;
    }

    /**
      * Sets the value of the 'sinkFileDescriptor' field.
      * @param value The value of 'sinkFileDescriptor'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder setSinkFileDescriptor(int value) {
      validate(fields()[3], value);
      this.sinkFileDescriptor = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'sinkFileDescriptor' field has been set.
      * @return True if the 'sinkFileDescriptor' field has been set, false otherwise.
      */
    public boolean hasSinkFileDescriptor() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'sinkFileDescriptor' field.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.UnnamedPipeObject.Builder clearSinkFileDescriptor() {
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    public UnnamedPipeObject build() {
      try {
        UnnamedPipeObject record = new UnnamedPipeObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.AbstractObject) defaultValue(fields()[1]);
        }
        record.sourceFileDescriptor = fieldSetFlags()[2] ? this.sourceFileDescriptor : (java.lang.Integer) defaultValue(fields()[2]);
        record.sinkFileDescriptor = fieldSetFlags()[3] ? this.sinkFileDescriptor : (java.lang.Integer) defaultValue(fields()[3]);
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
