/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.bbn.tc.schema.avro;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** * A principal is a local user
     * TODO: extend to include remote principals
     * TODO: what happens when the user information changes (are we tracking versions?)
     * TODO: Authentication mechanisms: are TA1s providing that information and how? */
@org.apache.avro.specific.AvroGenerated
public class Principal extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 4774945502184754166L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Principal\",\"namespace\":\"com.bbn.tc.schema.avro\",\"doc\":\"* A principal is a local user\\n     * TODO: extend to include remote principals\\n     * TODO: what happens when the user information changes (are we tracking versions?)\\n     * TODO: Authentication mechanisms: are TA1s providing that information and how?\",\"fields\":[{\"name\":\"uuid\",\"type\":{\"type\":\"fixed\",\"name\":\"UUID\",\"size\":16},\"doc\":\"A unique id for the principal\"},{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"PrincipalType\",\"doc\":\"* PrincipalType identifies the type of user: either local to the\\n     * host, or remote users/systems.\",\"symbols\":[\"PRINCIPAL_LOCAL\",\"PRINCIPAL_REMOTE\"]},\"doc\":\"The type of the principal, local by default\",\"default\":\"PRINCIPAL_LOCAL\"},{\"name\":\"userId\",\"type\":\"string\",\"doc\":\"The operating system identifier associated with the user\"},{\"name\":\"username\",\"type\":[\"null\",\"string\"],\"doc\":\"Human-readable string identifier, such as username (Optional)\",\"default\":null},{\"name\":\"groupIds\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"doc\":\"The ids of the groups which this user is part of\"},{\"name\":\"properties\",\"type\":[\"null\",{\"type\":\"map\",\"values\":\"string\"}],\"doc\":\"* Arbitrary key, value pairs describing the entity.\\n         * NOTE: This attribute is meant as a temporary place holder for items that\\n         * will become first-class attributes in the next CDM version.\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** A unique id for the principal */
  @Deprecated public com.bbn.tc.schema.avro.UUID uuid;
  /** The type of the principal, local by default */
  @Deprecated public com.bbn.tc.schema.avro.PrincipalType type;
  /** The operating system identifier associated with the user */
  @Deprecated public java.lang.CharSequence userId;
  /** Human-readable string identifier, such as username (Optional) */
  @Deprecated public java.lang.CharSequence username;
  /** The ids of the groups which this user is part of */
  @Deprecated public java.util.List<java.lang.CharSequence> groupIds;
  /** * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version. */
  @Deprecated public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> properties;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Principal() {}

  /**
   * All-args constructor.
   * @param uuid A unique id for the principal
   * @param type The type of the principal, local by default
   * @param userId The operating system identifier associated with the user
   * @param username Human-readable string identifier, such as username (Optional)
   * @param groupIds The ids of the groups which this user is part of
   * @param properties * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
   */
  public Principal(com.bbn.tc.schema.avro.UUID uuid, com.bbn.tc.schema.avro.PrincipalType type, java.lang.CharSequence userId, java.lang.CharSequence username, java.util.List<java.lang.CharSequence> groupIds, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> properties) {
    this.uuid = uuid;
    this.type = type;
    this.userId = userId;
    this.username = username;
    this.groupIds = groupIds;
    this.properties = properties;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return uuid;
    case 1: return type;
    case 2: return userId;
    case 3: return username;
    case 4: return groupIds;
    case 5: return properties;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: uuid = (com.bbn.tc.schema.avro.UUID)value$; break;
    case 1: type = (com.bbn.tc.schema.avro.PrincipalType)value$; break;
    case 2: userId = (java.lang.CharSequence)value$; break;
    case 3: username = (java.lang.CharSequence)value$; break;
    case 4: groupIds = (java.util.List<java.lang.CharSequence>)value$; break;
    case 5: properties = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'uuid' field.
   * @return A unique id for the principal
   */
  public com.bbn.tc.schema.avro.UUID getUuid() {
    return uuid;
  }

  /**
   * Sets the value of the 'uuid' field.
   * A unique id for the principal
   * @param value the value to set.
   */
  public void setUuid(com.bbn.tc.schema.avro.UUID value) {
    this.uuid = value;
  }

  /**
   * Gets the value of the 'type' field.
   * @return The type of the principal, local by default
   */
  public com.bbn.tc.schema.avro.PrincipalType getType() {
    return type;
  }

  /**
   * Sets the value of the 'type' field.
   * The type of the principal, local by default
   * @param value the value to set.
   */
  public void setType(com.bbn.tc.schema.avro.PrincipalType value) {
    this.type = value;
  }

  /**
   * Gets the value of the 'userId' field.
   * @return The operating system identifier associated with the user
   */
  public java.lang.CharSequence getUserId() {
    return userId;
  }

  /**
   * Sets the value of the 'userId' field.
   * The operating system identifier associated with the user
   * @param value the value to set.
   */
  public void setUserId(java.lang.CharSequence value) {
    this.userId = value;
  }

  /**
   * Gets the value of the 'username' field.
   * @return Human-readable string identifier, such as username (Optional)
   */
  public java.lang.CharSequence getUsername() {
    return username;
  }

  /**
   * Sets the value of the 'username' field.
   * Human-readable string identifier, such as username (Optional)
   * @param value the value to set.
   */
  public void setUsername(java.lang.CharSequence value) {
    this.username = value;
  }

  /**
   * Gets the value of the 'groupIds' field.
   * @return The ids of the groups which this user is part of
   */
  public java.util.List<java.lang.CharSequence> getGroupIds() {
    return groupIds;
  }

  /**
   * Sets the value of the 'groupIds' field.
   * The ids of the groups which this user is part of
   * @param value the value to set.
   */
  public void setGroupIds(java.util.List<java.lang.CharSequence> value) {
    this.groupIds = value;
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
   * Creates a new Principal RecordBuilder.
   * @return A new Principal RecordBuilder
   */
  public static com.bbn.tc.schema.avro.Principal.Builder newBuilder() {
    return new com.bbn.tc.schema.avro.Principal.Builder();
  }

  /**
   * Creates a new Principal RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Principal RecordBuilder
   */
  public static com.bbn.tc.schema.avro.Principal.Builder newBuilder(com.bbn.tc.schema.avro.Principal.Builder other) {
    return new com.bbn.tc.schema.avro.Principal.Builder(other);
  }

  /**
   * Creates a new Principal RecordBuilder by copying an existing Principal instance.
   * @param other The existing instance to copy.
   * @return A new Principal RecordBuilder
   */
  public static com.bbn.tc.schema.avro.Principal.Builder newBuilder(com.bbn.tc.schema.avro.Principal other) {
    return new com.bbn.tc.schema.avro.Principal.Builder(other);
  }

  /**
   * RecordBuilder for Principal instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Principal>
    implements org.apache.avro.data.RecordBuilder<Principal> {

    /** A unique id for the principal */
    private com.bbn.tc.schema.avro.UUID uuid;
    /** The type of the principal, local by default */
    private com.bbn.tc.schema.avro.PrincipalType type;
    /** The operating system identifier associated with the user */
    private java.lang.CharSequence userId;
    /** Human-readable string identifier, such as username (Optional) */
    private java.lang.CharSequence username;
    /** The ids of the groups which this user is part of */
    private java.util.List<java.lang.CharSequence> groupIds;
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
    private Builder(com.bbn.tc.schema.avro.Principal.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.type)) {
        this.type = data().deepCopy(fields()[1].schema(), other.type);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.userId)) {
        this.userId = data().deepCopy(fields()[2].schema(), other.userId);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.username)) {
        this.username = data().deepCopy(fields()[3].schema(), other.username);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.groupIds)) {
        this.groupIds = data().deepCopy(fields()[4].schema(), other.groupIds);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.properties)) {
        this.properties = data().deepCopy(fields()[5].schema(), other.properties);
        fieldSetFlags()[5] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing Principal instance
     * @param other The existing instance to copy.
     */
    private Builder(com.bbn.tc.schema.avro.Principal other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.uuid)) {
        this.uuid = data().deepCopy(fields()[0].schema(), other.uuid);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.type)) {
        this.type = data().deepCopy(fields()[1].schema(), other.type);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.userId)) {
        this.userId = data().deepCopy(fields()[2].schema(), other.userId);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.username)) {
        this.username = data().deepCopy(fields()[3].schema(), other.username);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.groupIds)) {
        this.groupIds = data().deepCopy(fields()[4].schema(), other.groupIds);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.properties)) {
        this.properties = data().deepCopy(fields()[5].schema(), other.properties);
        fieldSetFlags()[5] = true;
      }
    }

    /**
      * Gets the value of the 'uuid' field.
      * A unique id for the principal
      * @return The value.
      */
    public com.bbn.tc.schema.avro.UUID getUuid() {
      return uuid;
    }

    /**
      * Sets the value of the 'uuid' field.
      * A unique id for the principal
      * @param value The value of 'uuid'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder setUuid(com.bbn.tc.schema.avro.UUID value) {
      validate(fields()[0], value);
      this.uuid = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'uuid' field has been set.
      * A unique id for the principal
      * @return True if the 'uuid' field has been set, false otherwise.
      */
    public boolean hasUuid() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'uuid' field.
      * A unique id for the principal
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearUuid() {
      uuid = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'type' field.
      * The type of the principal, local by default
      * @return The value.
      */
    public com.bbn.tc.schema.avro.PrincipalType getType() {
      return type;
    }

    /**
      * Sets the value of the 'type' field.
      * The type of the principal, local by default
      * @param value The value of 'type'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder setType(com.bbn.tc.schema.avro.PrincipalType value) {
      validate(fields()[1], value);
      this.type = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'type' field has been set.
      * The type of the principal, local by default
      * @return True if the 'type' field has been set, false otherwise.
      */
    public boolean hasType() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'type' field.
      * The type of the principal, local by default
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearType() {
      type = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'userId' field.
      * The operating system identifier associated with the user
      * @return The value.
      */
    public java.lang.CharSequence getUserId() {
      return userId;
    }

    /**
      * Sets the value of the 'userId' field.
      * The operating system identifier associated with the user
      * @param value The value of 'userId'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder setUserId(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.userId = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'userId' field has been set.
      * The operating system identifier associated with the user
      * @return True if the 'userId' field has been set, false otherwise.
      */
    public boolean hasUserId() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'userId' field.
      * The operating system identifier associated with the user
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearUserId() {
      userId = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'username' field.
      * Human-readable string identifier, such as username (Optional)
      * @return The value.
      */
    public java.lang.CharSequence getUsername() {
      return username;
    }

    /**
      * Sets the value of the 'username' field.
      * Human-readable string identifier, such as username (Optional)
      * @param value The value of 'username'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder setUsername(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.username = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'username' field has been set.
      * Human-readable string identifier, such as username (Optional)
      * @return True if the 'username' field has been set, false otherwise.
      */
    public boolean hasUsername() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'username' field.
      * Human-readable string identifier, such as username (Optional)
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearUsername() {
      username = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'groupIds' field.
      * The ids of the groups which this user is part of
      * @return The value.
      */
    public java.util.List<java.lang.CharSequence> getGroupIds() {
      return groupIds;
    }

    /**
      * Sets the value of the 'groupIds' field.
      * The ids of the groups which this user is part of
      * @param value The value of 'groupIds'.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder setGroupIds(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[4], value);
      this.groupIds = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'groupIds' field has been set.
      * The ids of the groups which this user is part of
      * @return True if the 'groupIds' field has been set, false otherwise.
      */
    public boolean hasGroupIds() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'groupIds' field.
      * The ids of the groups which this user is part of
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearGroupIds() {
      groupIds = null;
      fieldSetFlags()[4] = false;
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
    public com.bbn.tc.schema.avro.Principal.Builder setProperties(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[5], value);
      this.properties = value;
      fieldSetFlags()[5] = true;
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
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'properties' field.
      * * Arbitrary key, value pairs describing the entity.
         * NOTE: This attribute is meant as a temporary place holder for items that
         * will become first-class attributes in the next CDM version.
      * @return This builder.
      */
    public com.bbn.tc.schema.avro.Principal.Builder clearProperties() {
      properties = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public Principal build() {
      try {
        Principal record = new Principal();
        record.uuid = fieldSetFlags()[0] ? this.uuid : (com.bbn.tc.schema.avro.UUID) defaultValue(fields()[0]);
        record.type = fieldSetFlags()[1] ? this.type : (com.bbn.tc.schema.avro.PrincipalType) defaultValue(fields()[1]);
        record.userId = fieldSetFlags()[2] ? this.userId : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.username = fieldSetFlags()[3] ? this.username : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.groupIds = fieldSetFlags()[4] ? this.groupIds : (java.util.List<java.lang.CharSequence>) defaultValue(fields()[4]);
        record.properties = fieldSetFlags()[5] ? this.properties : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) defaultValue(fields()[5]);
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
