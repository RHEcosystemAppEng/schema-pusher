package com.redhat.schema.pusher.avro;

import static java.util.Objects.requireNonNull;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDe;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * This a custom version of both {@code io.confluent.kafka.serializers.KafkaAvroSerializer} and
 * {@code io.confluent.kafka.serializers.AbstractKafkaAvroSerializer}.
 * The original implementation uses {@code org.apache.avro.io.DatumWriter} to produce a message
 * based on the schema.
 * Since this application is used for producing schema messages only, the original implementation,
 * although worked, threw exceptions that needed to be hard-coded ignored. Hence, we created this
 * custom implementation and ignored no exception.
 */
public final class AvroCustomSerializer extends AbstractKafkaSchemaSerDe implements Serializer<Object> {
  private boolean isKey;

  private boolean normalizeSchema;
  private boolean autoRegisterSchema;
  private boolean removeJavaProperties;
  private int useSchemaId = -1;
  private boolean idCompatStrict;
  private boolean useLatestVersion;
  private boolean latestCompatStrict;
  private boolean avroReflectionAllowNull = false;

  public AvroCustomSerializer() {
    //
  }

  /* *************************************************************************** *
   * Originated from {@code io.confluent.kafka.serializers.KafkaAvroSerializer}. *
   * *************************************************************************** */
  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    this.isKey = isKey;
    configure(new KafkaAvroSerializerConfig(configs));
  }

  @Override
  public byte[] serialize(final String topic, final Object object) {
    requireNonNull(object);
    var parshedSchema = AvroSchemaUtils.getSchema(object, useSchemaReflection, avroReflectionAllowNull, removeJavaProperties);
    var schema = new AvroSchema(parshedSchema);
    var subjectName = getSubjectName(topic, isKey, object, schema);
    return serializeImpl(subjectName, schema);
  }

  @Override
  public void close() {
    //
  }

  /* *********************************************************************************** *
   * Originated from {@code io.confluent.kafka.serializers.AbstractKafkaAvroSerializer}. *
   * *********************************************************************************** */
  private void configure(final KafkaAvroSerializerConfig config) {
    configureClientProperties(config, new AvroSchemaProvider());
    normalizeSchema = config.normalizeSchema();
    autoRegisterSchema = config.autoRegisterSchema();
    removeJavaProperties = config.getBoolean(KafkaAvroSerializerConfig.AVRO_REMOVE_JAVA_PROPS_CONFIG);
    useSchemaId = config.useSchemaId();
    idCompatStrict = config.getIdCompatibilityStrict();
    useLatestVersion = config.useLatestVersion();
    latestCompatStrict = config.getLatestCompatibilityStrict();
    avroReflectionAllowNull = config.getBoolean(KafkaAvroSerializerConfig.AVRO_REFLECTION_ALLOW_NULL_CONFIG);
  }

  // TODO the schema argument should be finalized
  private byte[] serializeImpl(final String subject, AvroSchema schema)
      throws SerializationException, InvalidConfigurationException {
    requireNonNull(schemaRegistry);

    String restClientErrorMsg = "";
    try {
      int id;
      if (autoRegisterSchema) {
        restClientErrorMsg = "Error registering Avro schema";
        id = schemaRegistry.register(subject, schema, normalizeSchema);
      } else if (useSchemaId >= 0) {
        restClientErrorMsg = "Error retrieving schema ID";
        schema = (AvroSchema)
            lookupSchemaBySubjectAndId(subject, useSchemaId, schema, idCompatStrict);
        id = schemaRegistry.getId(subject, schema);
      } else if (useLatestVersion) {
        restClientErrorMsg = "Error retrieving latest version of Avro schema";
        schema = (AvroSchema) lookupLatestVersion(subject, schema, latestCompatStrict);
        id = schemaRegistry.getId(subject, schema, normalizeSchema);
      } else {
        restClientErrorMsg = "Error retrieving Avro schema";
        id = schemaRegistry.getId(subject, schema, normalizeSchema);
      }
      var out = new ByteArrayOutputStream();
      out.write(MAGIC_BYTE);
      out.write(ByteBuffer.allocate(idSize).putInt(id).array());

      var bytes = out.toByteArray();
      out.close();
      return bytes;
    } catch (final IOException | RuntimeException exc) {
      throw new SerializationException("Error serializing Avro message", exc);
    } catch (final RestClientException rce) {
      throw toKafkaException(rce, restClientErrorMsg + schema);
    }
  }
}
