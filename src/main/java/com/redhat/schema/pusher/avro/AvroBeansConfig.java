package com.redhat.schema.pusher.avro;

import java.util.Properties;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * DI beans configuration class for the AVRO implementation of the schema pusher, this class should
 * be used by the DI context to start this implementation.
 */
@Configuration
@ComponentScan(basePackages = "com.redhat.schema.pusher.avro", lazyInit = true)
public class AvroBeansConfig {
  /**
   * Prototype Bean for creating a {@code org.apache.kafka.clients.producer.KafkaProducer} instance.
   * @param producerProps the {@link Properties} instance to be used by the producer.
   * @return the {@code org.apache.kafka.clients.producer.KafkaProducer} instance.
   */
  @Bean
  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  public KafkaProducer<String, IndexedRecord> kafkaProducer(final Properties producerProps) {
    return new KafkaProducer<>(producerProps);
  }
}
