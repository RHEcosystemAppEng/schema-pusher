package com.redhat.schema.avro;

import com.redhat.schema.AbstractSchemaPusher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * The AVRO Pusher wraps {@link AbstractSchemaPusher} using {@code String} as the key type and
 * {@code org.apache.avro.generic.IndexedRecord} as the value type.
 */
public final class AvroPusher extends AbstractSchemaPusher<String, IndexedRecord> {
  /**
   * Constructor takes a producer and a parser.
   * @param producer an {@link AvroProducer} instance for producing messages.
   */
  public AvroPusher(final KafkaProducer<String, IndexedRecord> producer) {
    super(producer);
  }

  /**
   * {@inheritDoc}
   * {@code https://avro.apache.org/docs/current/api/java/org/apache/avro/Schema.Parser.html}.
   * The {@code org.apache.avro.Schema.Parser} is part of the schema, using the same parser for
   * multiple topics may yield {@code org.apache.avro.SchemaParseException: Can't redefine}.
   * Because we support multiple topics, the {@code Parser} should be instantiated per each topic.
   * For contractual reasons, you can use the overloaded {@link #push(Parser, String, String)} to
   * use do so.
   * Invoking this method will instantiate a new {@code Parser} per each invocation.
   */
  @Override
  public Future<RecordMetadata> push(final Path schemaPath, final String topic) throws IOException {
    return push(new Parser(), schemaPath, topic);
  }

  /**
   * Overloaded for sharing {@code org.apache.avro.Schema.Parser} instances.
   * @see #push(String, String)
   * @param parser the shared {@code org.apache.avro.Schema.Parser} instance.
   * @param schemaPath the path of the schema file.
   * @param topic the topic to produce the schema to.
   * @return a {@code org.apache.kafka.clients.producer.RecordMetadata} {@link Future}.
   * @throws IOException when failure occures while reading the file.
   */
  public Future<RecordMetadata> push(final Parser parser, final Path schemaPath, final String topic) throws IOException {
    var schema = parser.parse(Files.newInputStream(schemaPath));
    var avroRec = new GenericData.Record(schema);
    var prodRec = new ProducerRecord<String, IndexedRecord>(topic, avroRec);
    return getProducer().send(prodRec);
  }
}
