package com.redhat.schema.pusher.avro;

import static com.redhat.schema.pusher.UrlUtils.cleanUrlEnd;
import static com.redhat.schema.pusher.UrlUtils.concatConfluentMap;
import static com.redhat.schema.pusher.UrlUtils.isSecured;
import static java.util.Objects.nonNull;

import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.ReturnCode;
import com.redhat.schema.pusher.SchemaPusher;
import com.redhat.schema.pusher.TopicAndSchema;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
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

  private static final String STORE_TYPE_PKCS12 = "PKCS12";

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
  public ReturnCode push(final List<TopicAndSchema> topicAndSchemaRecords) {
    LOGGER.info("loading the producer");
    try (var producer = context.getBean(KafkaProducer.class, producerProps)) {
      topicAndSchemaRecords.parallelStream().forEach(rec -> {
        var fileName = rec.schema().toFile().getName();
        LOGGER.info(
            () -> String.format(
              "parallel stream for topic '%s' and schema '%s' running on thread '%s'",
              rec.topic(),
              fileName,
              Thread.currentThread().getName()));
        try {
          var parser = new Parser();
          var schema = parser.parse(Files.newInputStream(rec.schema()));
          var avroRec = new GenericData.Record(schema);
          var prodRec = new ProducerRecord<String, IndexedRecord>(rec.topic(), avroRec);
          var callback = context.getBean(Callback.class, LOGGER, fileName, rec.topic());
          producer.send(prodRec, callback);
        } catch (final IOException exc) {
          LOGGER.log(
              Level.SEVERE,
              exc,
              () -> String.format("failed to push '%s' to topic '%s'", fileName, rec.topic()));
        }
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
  // CHECKSTYLE.OFF: VariableDeclarationUsageDistance
  private Properties createProps(final PushCli cli) {
    // get info from the cli
    var kafkaBootstrapUrl = cleanUrlEnd.apply(cli.getKafkaBootstrap());
    var registryUrl = cleanUrlEnd.andThen(concatConfluentMap).apply(cli.getServiceRegistry());
    var namingStrategy = cli.getNamingStrategy();
    var propertyAggregators = cli.getPropertyAggregators();
    var truststoreInfo = cli.getTruststoreInfo();
    var keystoreInfo = cli.getKeystoreInfo();
    // create, populate, and return the properties instance
    var props = new Properties();
    // standard configuration (can be replaced with custom properties by the user)
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    // if supplied properties info
    if (nonNull(propertyAggregators)) {
      // add custom producer properties set by the user
      propertyAggregators.forEach(agg -> props.put(agg.getPropertyKey(), agg.getPropertyValue()));
    }
    // the rest of the values will overwrite custom properties set by the user
    // set the kafka bootstrap and rh service registry urls
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapUrl);
    props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
    // use the string serializer for the keys
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    // use custom serializer for only serializing the schema and not the object
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroCustomSerializer.class);
    // set selected naming strategy
    props.put(
        AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY, namingStrategy.getStrategy());
    // if the bootstrap server is secured
    if (isSecured(kafkaBootstrapUrl)) {
      // set SSL as the protocol
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
      // if supplied truststore info
      if (nonNull(truststoreInfo)) {
        var truststoreFile = truststoreInfo.getTruststoreFile();
        var truststorePassword = truststoreInfo.getTruststorePassword();
        if (nonNull(truststoreFile) && nonNull(truststorePassword)) {
          // set truststore configuration
          props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreFile);
          props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
          props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, STORE_TYPE_PKCS12);
        }
      }
      // if supplied keystore info
      if (nonNull(keystoreInfo)) {
        var keystoreFile = keystoreInfo.getKeystoreFile();
        var keystorePassword = keystoreInfo.getKeystorePassword();
        if (nonNull(keystoreFile) && nonNull(keystorePassword)) {
          // set keystore configuration
          props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreFile);
          props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
          props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, STORE_TYPE_PKCS12);
        }
      }
    }
    return props;
  }
}
