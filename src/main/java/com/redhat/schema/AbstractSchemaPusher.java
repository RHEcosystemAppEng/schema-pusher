package com.redhat.schema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Abstract class for enacpsulating {@code org.apache.kafka.clients.producer.KafkaProducer} instances.
 * @param <K> the key type.
 * @param <V> the value type.
 */
public abstract class AbstractSchemaPusher<K, V> {
  private final KafkaProducer<K, V> producer;

  /**
   * Constructor takes a {@code org.apache.kafka.clients.producer.KafkaProducer}.
   * @param setProducer the producer instance for encapsulating.
   */
  protected AbstractSchemaPusher(final KafkaProducer<K, V> setProducer) {
    this.producer = setProducer;
  }

  /**
   * Abstract method for pusing schemas files using the producer.
   * @param schemaPath the path of the schema file.
   * @param topic the topic to produce the schema to.
   * @return a {@code org.apache.kafka.clients.producer.RecordMetadata} {@link Future}.
   * @throws IOException when failure occures while reading the file.
   */
  public abstract Future<RecordMetadata> push(final Path schemaPath, final String topic) throws IOException;

  /**
   * Getter for the enacpsulated {@code org.apache.kafka.clients.producer.KafkaProducer}.
   * @return the enacpsulated producer.
   */
  protected final KafkaProducer<K, V> getProducer() {
    return this.producer;
  }
}
