package com.redhat.schema.avro;

import com.redhat.schema.PropertiesAggregator;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 * The AVRO Producer wraps {@code org.apache.kafka.clients.producer.KafkaProducer.KafkaProducer}
 * using {@link String} as the key type and {@code org.apache.avro.generic.IndexedRecord} as the
 * value type.
 */
public final class AvroProducer extends KafkaProducer<String, IndexedRecord> {
  /**
   * Constructor takes a {@link PropertiesAggregator}.
   * @param propertiesAggregator encapsulating a {@link Properties} object.
   */
  public AvroProducer(final PropertiesAggregator propertiesAggregator) {
    super(propertiesAggregator.getProperties());
  }
}
