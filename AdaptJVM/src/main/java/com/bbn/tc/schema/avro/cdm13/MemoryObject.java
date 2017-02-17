/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro.cdm13;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * Represents a page in memory. Instantiates an AbstractObject.
 * TODO: is memory really an object (with permissions and so on) or is it a * transient data? */
@org.apache.avro.specific.AvroGenerated
public class MemoryObject extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 2718317826791287782L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"MemoryObject\",\"namespace\":\"com.bbn.tc.schema.avro.cdm13\",\"doc\":\"* Represents a page in memory. Instantiates an AbstractObject.\\n * TODO: is memory really an object (with permissions and so on) or is it a * transient data?\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"Universally unique identifier for the object\"},{\"name\":\"baseObject\",\"type\":{\"type\":\"record\",\"name\":\"AbstractObject\",\"doc\":\"*  Objects, in general, represent data sources and sinks which could include sockets, files,\\n     *  memory, and any data in general that can be an input and/or output to an event.\\n     *  This record is intended to be abstract i.e., one should not instantiate an Object\\n     *  but rather instantiate one of its sub types File, NetFlow, of Memory\",\"fields\":[{\"name\":\"source\",\"type\":{\"type\":\"enum\",\"name\":\"InstrumentationSource\",\"doc\":\"* SOURCE_LINUX_AUDIT_TRACE,          from Linux /dev/audit\\n * SOURCE_LINUX_PROC_TRACE,           from Linux's /proc\\n     * * SOURCE_LINUX_BEEP_TRACE,           from BEEP instrumentation\\n     * * SOURCE_FREEBSD_OPENBSM_TRACE,      from FreeBSD openBSM\\n     * * SOURCE_ANDROID_JAVA_CLEARSCOPE,    from android java instrumentation\\n     * * SOURCE_ANDROID_NATIVE_CLEARSCOPE,  from android's native instrumentation\\n * * SOURCE_FREEBSD_DTRACE_CADETS, SOURCE_FREEBSD_TESLA_CADETS  for CADETS * freebsd instrumentation\\n     * SOURCE_FREEBSD_LOOM_CADETS, * SOURCE_FREEBSD_MACIF_CADETS    for CADETS freebsd instrumentation\\n     * * SOURCE_LINUX_THEIA                 from the GATech THEIA instrumentation * source\\n     * SOURCE_WINDOWS_FIVEDIRECTIONS      for the fivedirections * windows events\",\"symbols\":[\"SOURCE_LINUX_AUDIT_TRACE\",\"SOURCE_LINUX_PROC_TRACE\",\"SOURCE_LINUX_BEEP_TRACE\",\"SOURCE_FREEBSD_OPENBSM_TRACE\",\"SOURCE_ANDROID_JAVA_CLEARSCOPE\",\"SOURCE_ANDROID_NATIVE_CLEARSCOPE\",\"SOURCE_FREEBSD_DTRACE_CADETS\",\"SOURCE_FREEBSD_TESLA_CADETS\",\"SOURCE_FREEBSD_LOOM_CADETS\",\"SOURCE_FREEBSD_MACIF_CADETS\",\"SOURCE_WINDOWS_DIFT_FAROS\",\"SOURCE_LINUX_THEIA\",\"SOURCE_WINDOWS_FIVEDIRECTIONS\"]},\"doc\":\"The source that emitted the object, see InstrumentationSource\"},{\"name\":\"permission\",\"type\":[\"null\",{\"type\":\"fixed\",\"name\":\"SHORT\",\"size\":2}],\"doc\":\"Permission bits defined over the object (Optional)\",\"default\":null},{\"name\":\"lastTimestampMicros\",\"type\":[\"null\",\"long\"],\"doc\":\"* The timestamp when the object was last modified (Optional).\\n        * A timestamp stores the number of microseconds from the unix epoch, 1 January 1970 00:00:00.000000 UTC.\",\"default\":null},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"Arbitrary key, value pairs describing the entity\",\"default\":null}]},\"doc\":\"The base object attributes\"},{\"name\":\"memoryAddress\",\"type\":\"long\",\"doc\":\"The location in memory\"},{\"name\":\"pageNumber\",\"type\":[\"null\",\"long\"],\"doc\":\"The memory page number\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** Universally unique identifier for the object */
  @Deprecated public com.bbn.tc.schema.avro.cdm13.UUID uuid;
  /** The base object attributes */
  @Deprecated public com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject;
  /** The location in memory */
  @Deprecated public long memoryAddress;
  /** The memory page number */
  @Deprecated public java.lang.Long pageNumber;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public MemoryObject() {}

  /**
   * All-args constructor.
   * @param uuid Universally unique identifier for the object
   * @param baseObject The base object attributes
   * @param memoryAddress The location in memory
   * @param pageNumber The memory page number
   */
  public MemoryObject(com.bbn.tc.schema.avro.cdm13.UUID uuid, com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject, java.lang.Long memoryAddress, java.lang.Long pageNumber) {
    this.uuid = uuid;
    this.baseObject = baseObject;
    this.memoryAddress = memoryAddress;
    this.pageNumber = pageNumber;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return baseObject;
    case 2: return memoryAddress;
    case 3: return pageNumber;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.cdm13.UUID)value$; break;
    case 1: baseObject = (com.bbn.tc.schema.avro.cdm13.AbstractObject)value$; break;
    case 2: memoryAddress = (java.lang.Long)value$; break;
    case 3: pageNumber = (java.lang.Long)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return Universally unique identifier for the object
   */
  public com.bbn.tc.schema.avro.cdm13.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * Universally unique identifier for the object
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.cdm13.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'baseObject' field.
   * @return The base object attributes
   */
  public com.bbn.tc.schema.avro.cdm13.AbstractObject getBaseObject() {
    return baseObject;
  }

  /**
   * Sets the value of the 'baseObject' field.
   * The base object attributes
   * @param value the value to set.
   */
  public void setBaseObject(com.bbn.tc.schema.avro.cdm13.AbstractObject value) {
    this.baseObject = value;
  }

  /**
   * Gets the value of the 'memoryAddress' field.
   * @return The location in memory
   */
  public java.lang.Long getMemoryAddress() {
    return memoryAddress;
  }

  /**
   * Sets the value of the 'memoryAddress' field.
   * The location in memory
   * @param value the value to set.
   */
  public void setMemoryAddress(java.lang.Long value) {
    this.memoryAddress = value;
  }

  /**
   * Gets the value of the 'pageNumber' field.
   * @return The memory page number
   */
  public java.lang.Long getPageNumber() {
    return pageNumber;
  }

  /**
   * Sets the value of the 'pageNumber' field.
   * The memory page number
   * @param value the value to set.
   */
  public void setPageNumber(java.lang.Long value) {
    this.pageNumber = value;
  }

  /**
   * Creates a new MemoryObject RecordBuilder.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder();
  }

  /**
   * Creates a new MemoryObject RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder other) {
    return new com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder(other);
  }

  /**
   * Creates a new MemoryObject RecordBuilder by copying an existing MemoryObject instance.
   * @param other The existing instance to copy.
   * @return A new MemoryObject RecordBuilder
   */
  public static com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder newBuilder(com.bbn.tc.schema.avro.cdm13.MemoryObject other) {
    return new com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder(other);
  }

  /**
   * RecordBuilder for MemoryObject instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<MemoryObject>
    implements org.apache.avro.data.RecordBuilder<MemoryObject> {

    /** Universally unique identifier for the object */
    private com.bbn.tc.schema.avro.cdm13.UUID uuid;
    /** The base object attributes */
    private com.bbn.tc.schema.avro.cdm13.AbstractObject baseObject;
    private com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder baseObjectBuilder;
    /** The location in memory */
    private long memoryAddress;
    /** The memory page number */
    private java.lang.Long pageNumber;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder other) {
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
        this.baseObjectBuilder = com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder(other.getBaseObjectBuilder());
      }
      if (isValidValue(fields()[2], other.memoryAddress)) {
        this.memoryAddress = data().deepCopy(fields()[2].schema(), other.memoryAddress);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.pageNumber)) {
        this.pageNumber = data().deepCopy(fields()[3].schema(), other.pageNumber);
        fieldSetFlags()[3] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing MemoryObject instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.cdm13.MemoryObject other) {
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
      if (isValidValue(fields()[2], other.memoryAddress)) {
        this.memoryAddress = data().deepCopy(fields()[2].schema(), other.memoryAddress);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.pageNumber)) {
        this.pageNumber = data().deepCopy(fields()[3].schema(), other.pageNumber);
        fieldSetFlags()[3] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm13.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * Universally unique identifier for the object
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder setUuid(com.bbn.tc.schema.avro.cdm13.UUID value) {
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
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'baseObject' field.
      * The base object attributes
      * @return The value.
      */
    public com.bbn.tc.schema.avro.cdm13.AbstractObject getBaseObject() {
      return baseObject;
    }

    /**
      * Sets the value of the 'baseObject' field.
      * The base object attributes
      * @param value The value of 'baseObject'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder setBaseObject(com.bbn.tc.schema.avro.cdm13.AbstractObject value) {
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
    public com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder getBaseObjectBuilder() {
      if (baseObjectBuilder == null) {
        if (hasBaseObject()) {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder(baseObject));
        } else {
          setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.newBuilder());
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
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder setBaseObjectBuilder(com.bbn.tc.schema.avro.cdm13.AbstractObject.Builder value) {
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
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder clearBaseObject() {
      baseObject = null;
      baseObjectBuilder = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'memoryAddress' field.
      * The location in memory
      * @return The value.
      */
    public java.lang.Long getMemoryAddress() {
      return memoryAddress;
    }

    /**
      * Sets the value of the 'memoryAddress' field.
      * The location in memory
      * @param value The value of 'memoryAddress'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder setMemoryAddress(long value) {
      validate(fields()[2], value);
      this.memoryAddress = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'memoryAddress' field has been set.
      * The location in memory
      * @return True if the 'memoryAddress' field has been set, false otherwise.
      */
    public boolean hasMemoryAddress() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'memoryAddress' field.
      * The location in memory
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder clearMemoryAddress() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'pageNumber' field.
      * The memory page number
      * @return The value.
      */
    public java.lang.Long getPageNumber() {
      return pageNumber;
    }

    /**
      * Sets the value of the 'pageNumber' field.
      * The memory page number
      * @param value The value of 'pageNumber'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder setPageNumber(java.lang.Long value) {
      validate(fields()[3], value);
      this.pageNumber = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'pageNumber' field has been set.
      * The memory page number
      * @return True if the 'pageNumber' field has been set, false otherwise.
      */
    public boolean hasPageNumber() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'pageNumber' field.
      * The memory page number
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.cdm13.MemoryObject.Builder clearPageNumber() {
      pageNumber = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    public MemoryObject build() {
      try {
        MemoryObject record = new MemoryObject();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.cdm13.UUID) defaultValue(fields()[0]);
        if (baseObjectBuilder != null) {
          record.baseObject = this.baseObjectBuilder.build();
        } else {
          record.baseObject = fieldSetFlags()[1] ? this.baseObject : (com.bbn.tc.schema.avro.cdm13.AbstractObject) defaultValue(fields()[1]);
        }
        record.memoryAddress = fieldSetFlags()[2] ? this.memoryAddress : (java.lang.Long) defaultValue(fields()[2]);
        record.pageNumber = fieldSetFlags()[3] ? this.pageNumber : (java.lang.Long) defaultValue(fields()[3]);
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
