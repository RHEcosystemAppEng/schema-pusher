package com.redhat.schema.pusher.avro;

import static com.redhat.schema.pusher.UrlUtils.cleanUrlEnd;
import static com.redhat.schema.pusher.UrlUtils.concatConfluentMap;
import static com.redhat.schema.pusher.UrlUtils.isSecured;
import static java.util.Objects.nonNull;

import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.ReturnCode;
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
   * Constructor that takes the CLI instance and use it to create a {@link Properties} instance to
   * be used by the producer.
   *
   * @param cli the {@link PushCli} instance for configuring the kafka producer with its fields.
   */
  public AvroSchemaPusher(final PushCli cli) {
    this.producerProps = createProps(cli);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ReturnCode push(final List<String> topics, final List<Path> schemas) {
    LOGGER.info("loading producer");
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
      return ReturnCode.SUCCESS;
    } catch (final Exception exc) {
      LOGGER.log(Level.SEVERE, exc, () -> "producing messages to failed");
      return ReturnCode.PRODUCER_ERROR;
    }
  }

  /**
   * Utility method for creating the set of properties to be used by the producer.
   *
   * @param cli the {@link PushCli} instance for creating the {@link Properties} instance with.
   * @return an instance of {@link Properties}.
   */
  private Properties createProps(final PushCli cli) {
    // get info from the cli
    var kafkaBootstrapUrl = cli.getKafkaBootstrap();
    var registryUrl = cleanUrlEnd.andThen(concatConfluentMap).apply(cli.getServiceRegistry());
    var namingStrategy = cli.getNamingStrategy();
    var selfSignedInfo = cli.getSelfSignedInfo();
    // create, populate, and return the properties instance
    var props = new Properties();
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
    // if the bootstrap server is secured (standard for amq)
    if (isSecured(kafkaBootstrapUrl)) {
      // set SSL as the protocol
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
      // if supplied truststore and password
      if (nonNull(selfSignedInfo)) {
        var kafkaTruststorePath = cli.getSelfSignedInfo().getKafkaTruststoreJksPath();
        var kafkaTruststorePassword = cli.getSelfSignedInfo().getKafkaTruststorePassword();
        if (nonNull(kafkaTruststorePath) && nonNull(kafkaTruststorePassword)) {
          props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaTruststorePath);
          props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaTruststorePassword);
        }
      }

    }
    return props;
  }
}
