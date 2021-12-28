package com.redhat.schema.avro;

import static com.redhat.schema.UrlUtils.cleanUrlEnd;
import static com.redhat.schema.UrlUtils.concatConfluentMap;
import static com.redhat.schema.UrlUtils.isSecured;

import com.redhat.schema.NamingStrategy;
import com.redhat.schema.PropertiesAggregator;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * The AVRP Properties implements {@link PropertiesAggregator}, encapsulating a {@link Properties}
 * instance overloaded with the required properties for producing messages to the service registry.
 */
public final class AvroProperties implements PropertiesAggregator {
  private final Properties props;

  /**
   * Constructor instantiating the {@link Properties} class and overload it with properties for the
   * produdcer.
   * @param kafkaBootstrapUrl Kafka's Bootstrap url.
   * @param serviceRegistryUrl Red Hat's Service Registry url.
   * @param naming the {@link NamingStrategy} for the produced message's subject.
   */
  public AvroProperties(
      final String kafkaBootstrapUrl,
      final String serviceRegistryUrl,
      final NamingStrategy namingStrategy) {
    this.props = new Properties();
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
    props.put(AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, namingStrategy.getStrategy());
    // if the bootstrap server is secured (standard for amq) set SSL as the protocol
    props.put(
      CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
      isSecured(kafkaBootstrapUrl) ? "SSL" : CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);

    // TODO these are for development purposes - remove this
    if (Boolean.parseBoolean(System.getenv("IN_DEV_MODE"))) {
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, System.getenv("SSL_TRUSTSTORE_LOCATION_CONFIG"));
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("SSL_TRUSTSTORE_PASSWORD_CONFIG"));
    }
  }

  /**
   * Get the aggregated properties.
   * @return the encapsulated {@link Properties} instance.
   */
  public Properties getProperties() {
    return this.props;
  }
}
