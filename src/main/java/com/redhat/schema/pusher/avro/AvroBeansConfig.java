package com.redhat.schema.pusher.avro;

import java.util.Properties;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(basePackages = "com.redhat.schema.pusher.avro", lazyInit = true)
public class AvroBeansConfig {
  @Bean
  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  public KafkaProducer<String, IndexedRecord> kafkaProducer(final Properties producerProps){
    return new KafkaProducer<>(producerProps);
  }
}
