package com.redhat.schema.pusher.avro;

import static com.redhat.schema.pusher.UrlUtils.cleanUrlEnd;
import static com.redhat.schema.pusher.UrlUtils.concatConfluentMap;
import static com.redhat.schema.pusher.UrlUtils.isSecured;

import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.SchemaPusher;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * AVRO implementation of the {@link SchemaPusher}, provided the behavioural implementation for
 * pushing schemas to the registry.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class AvroSchemaPusher implements SchemaPusher {
  private static final Logger LOGGER = Logger.getLogger(AvroSchemaPusher.class.getName());

  @Autowired private ApplicationContext context;
  private final Properties producerProps;

  /**
   * Constructor the kafka bootstrap url, service registr url, and naming strategy and created the
   * {@link Properties} instance to be used by the producer.
   *
   * @param kafkaBootstrapUrl the {@link String} kafka bootstrap url.
   * @param serviceRegistryUrl the {@link String} service registry url.
   * @param namingStrategy the {@link NamingStrategy} member.
   */
  public AvroSchemaPusher(
      final String kafkaBootstrapUrl,
      final String serviceRegistryUrl,
      final NamingStrategy namingStrategy) {
    this.producerProps = createProps(kafkaBootstrapUrl, serviceRegistryUrl, namingStrategy);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void push(final List<String> topics, final List<Path> schemas) {
    try (var producer = context.getBean(KafkaProducer.class, producerProps)) {
      topics.parallelStream().forEach(topic -> {
        LOGGER.info(
          () -> String.format("parallel stream for topic '%s' running on thread '%s'", topic, Thread.currentThread().getName()));
        schemas.stream().forEach(path -> {
          try {
            var parser = new Parser();
            var schema = parser.parse(Files.newInputStream(path));
            var avroRec = new GenericData.Record(schema);
            var prodRec = new ProducerRecord<String, IndexedRecord>(topic, avroRec);
            producer.send(prodRec).get();
            LOGGER.info(
              () -> String.format("successfully pushed '%s' to topic '%s'", path.toFile().getName(), topic));
          } catch (final  ExecutionException | InterruptedException | IOException exc) {
            if (exc instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
            LOGGER.log(
              Level.SEVERE, exc, () -> String.format("failed to push '%s' to topic '%s'", path.toFile().getName(), topic));
          }
        });
      });
      producer.flush();
    }
  }

  /**
   * Utility method for creating the set of properties to be used by the producer.
   *
   * @param kafkaBootstrapUrl the {@link String} kafka bootstrap url.
   * @param serviceRegistryUrl the {@link String} service registry url.
   * @param namingStrategy the {@link NamingStrategy} member.
   * @return an instance of {@link Properties}.
   */
  private Properties createProps(
      final String kafkaBootstrapUrl,
      final String serviceRegistryUrl,
      final NamingStrategy namingStrategy) {
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
    props.put(
        AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, namingStrategy.getStrategy());
    // if the bootstrap server is secured (standard for amq) set SSL as the protocol
    props.put(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
        isSecured(kafkaBootstrapUrl) ? "SSL" : CommonClientConfigs.DEFAULT_SECURITY_PROTOCOL);

    // TODO these are for development purposes - remove this
    if (Boolean.parseBoolean(System.getenv("IN_DEV_MODE"))) {
      props.put(
          SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          System.getenv("SSL_TRUSTSTORE_LOCATION_CONFIG"));
      props.put(
          SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
          System.getenv("SSL_TRUSTSTORE_PASSWORD_CONFIG"));
    }
    return props;
  }
}
