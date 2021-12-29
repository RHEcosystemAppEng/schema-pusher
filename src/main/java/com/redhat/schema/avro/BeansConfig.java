package com.redhat.schema.avro;

import static com.redhat.schema.UrlUtils.cleanUrlEnd;
import static com.redhat.schema.UrlUtils.concatConfluentMap;
import static com.redhat.schema.UrlUtils.isSecured;

import com.redhat.schema.NamingStrategy;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import java.util.Properties;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Configuration
@ComponentScan(basePackages =  "com.redhat.schema.avro")
public class BeansConfig {
  @Bean
  @Lazy
  public Properties producerProperties(
      @Qualifier(Qualifiers.KAFKA_BOOTSTRAP_URL) final String kafkaBootstrapUrl,
      @Qualifier(Qualifiers.SERVICE_REGISTRY_URL) final String serviceRegistryUrl,
      @Qualifier(Qualifiers.NAMING_STRATEGY) final String namingStrategy) {
    var props = new Properties();
    var registryUrl = cleanUrlEnd.andThen(concatConfluentMap).apply(serviceRegistryUrl);
    // set the kafka bootstrap and rh service registry urls
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapUrl);
    props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
    // standard configuration
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    // use custom serializer for only serializing the schema and not the object
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroCustomSerializer.class);
    // set selected naming strategy
    props.put(AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, NamingStrategy.valueOf(namingStrategy).getStrategy());
    // if the bootstrap server is secured (standard for amq) set SSL as the protocol
    props.put(
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
      isSecured(kafkaBootstrapUrl) ? "SSL" : CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);

    // TODO these are for development purposes - remove this
    if (Boolean.parseBoolean(System.getenv("IN_DEV_MODE"))) {
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, System.getenv("SSL_TRUSTSTORE_LOCATION_CONFIG"));
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("SSL_TRUSTSTORE_PASSWORD_CONFIG"));
    }
    return props;
  }

  @Bean
  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  public KafkaProducer<String, IndexedRecord> kafkaProducer(
      @Qualifier("producerProperties") final Properties producerProperties){
    return new KafkaProducer<>(producerProperties);
  }

  public static final class Qualifiers {
    private Qualifiers() {
      // no constructor required
    }
    public static final String KAFKA_BOOTSTRAP_URL = "kafkaBootstrapUrl";
    public static final String SERVICE_REGISTRY_URL = "serviceRegistryUrl";
    public static final String NAMING_STRATEGY = "namingStrategy";
  }
}
