package com.redhat.schema.pusher;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/** Command line specification and utility methods, CLI implementations should extend this class. */
public abstract class PushCli implements Callable<Integer> {
  /* ******************** *
   * Command Line Options *
   * ******************** */
  @Option(
      names = {"-b", "--bootstrap-url"},
      description = "The url for Kafka's bootstrap server.",
      required = true)
  private String kafkaBootstrap;

  @Option(
      names = {"-r", "--registry-url"},
      description = "The url for Red Hat's service registry.",
      required = true)
  private String serviceRegistry;

  @Option(
      names = {"-n", "--naming-strategy"},
      description = "The subject naming strategy.",
      defaultValue = "TOPIC_RECORD")
  private NamingStrategy namingStrategy;

  @ArgGroup(exclusive = false, multiplicity = "1..*")
  private List<TopicSchemaAggregator> topicSchemaAggregators;

  @ArgGroup(exclusive = false, multiplicity = "0..1")
  private List<PropertyAggregator> propertyAggregators;

  @ArgGroup(exclusive = false, multiplicity = "0..1")
  private TruststoreInfo truststoreInfo;

  @ArgGroup(exclusive = false, multiplicity = "0..1")
  private KeystoreInfo keystoreInfo;

  /** Use for aggregating topics-schema_path pairs specified by the user. */
  public static final class TopicSchemaAggregator {
    @Option(
        names = {"-t", "--topic"},
        description = "The desired topic for the schema, correlated with a schema path.",
        required = true)
    private String topic;

    @Option(
        names = {"-s", "--schema-path"},
        description = "The schema path for the topic, correlated with a topic.",
        required = true
    )
    private Path schemaPath;

    /**
     * Returns the topic.
     * @return the {@link String} topic.
     */
    public String getTopic() {
      return this.topic;
    }

    /**
     * Returns the schema path.
     * @return the schema file {@link Path}.
     */
    public Path getSchemaPath() {
      return this.schemaPath;
    }
  }

  /** Use for aggregating propety key and value. */
  public static final class PropertyAggregator {
    @Option(
        names = {"--pk", "--property-key"},
        description = "Producer property key",
        required = true)
    private String propertyKey;

    @Option(
        names = {"--pv", "--property-value"},
        description = "Producer property value",
        required = true)
    private String propertyValue;

    /**
     * Returns the property key.
     * @return the property key.
     */
    public String getPropertyKey() {
      return this.propertyKey;
    }

    /**
     * Returns the property value.
     * @return the property value.
     */
    public String getPropertyValue() {
      return this.propertyValue;
    }
  }

  /** Use for binding the truststore pkcs12 path and password keys. */
  public static final class TruststoreInfo {
    @Option(
        names = {"--tf", "--truststore-file"},
        description = "The path for the truststore pkcs12 file for use with the Kafka producer",
        required = true)
    private String truststoreFile;

    @Option(
        names = {"--tp", "--truststore-password"},
        description = "The password for the truststore pkcs12 file for use with the Kafka producer",
        required = true)
    private String truststorePassword;

    /**
     * Returns the truststore file path.
     * @return the truststore file path.
     */
    public String getTruststoreFile() {
      return this.truststoreFile;
    }

    /**
     * Returns the truststore password.
     * @return the truststore password.
     */
    public String getTruststorePassword() {
      return this.truststorePassword;
    }
  }

  /** Use for binding the keystore pkcs12 path and password keys. */
  public static final class KeystoreInfo {
    @Option(
        names = {"--kf", "--keystore-file"},
        description = "The path for the keystore pkcs12 file for use with the Kafka producer",
        required = true)
    private String keystoreFile;

    @Option(
        names = {"--kp", "--keystore-password"},
        description = "The password for the keystore pkcs12 file for use with the Kafka producer",
        required = true)
    private String keystorePassword;

    /**
     * Returns the keystore file path.
     * @return the keystore file path.
     */
    public String getKeystoreFile() {
      return this.keystoreFile;
    }

    /**
     * Returns the keystore password.
     * @return the keystore password.
     */
    public String getKeystorePassword() {
      return this.keystorePassword;
    }
  }

  /* ************* *
   * Field Getters *
   * ************* */
  /**
   * Get Kafka's bootstrap url as specified by the user.
   *
   * @return a {@link String} url.
   */
  public String getKafkaBootstrap() {
    return this.kafkaBootstrap;
  }

  /**
   * Get Red Hat's service registry url as specified by the user.
   *
   * @return a {@link String} url.
   */
  public String getServiceRegistry() {
    return this.serviceRegistry;
  }

  /**
   * Get the naming strategy as specified by the user.
   *
   * @return a {@link NamingStrategy} member.
   */
  public NamingStrategy getNamingStrategy() {
    return this.namingStrategy;
  }

  /**
   * Get the list of topics-schema_path pairs as specified by the user.
   *
   * @return a {@link List} of {@link TopicSchemaAggregator} instances.
   */
  public List<TopicSchemaAggregator> getTopicSchemaAggregators() {
    return this.topicSchemaAggregators;
  }

  /**
   * Get the list of property aggregators as specified by the user.
   *
   * @return a {@link List} of {@link PropertyAggregator} instances.
   */
  public List<PropertyAggregator> getPropertyAggregators() {
    return this.propertyAggregators;
  }

  /**
   * Get the truststore pkcs12 information as specified by the user.
   *
   * @return a {@link TruststoreInfo} instance.
   */
  public TruststoreInfo getTruststoreInfo() {
    return this.truststoreInfo;
  }

  /**
   * Get the keystore pkcs12 information as specified by the user.
   *
   * @return a {@link KeystoreInfo} instance.
   */
  public KeystoreInfo getKeystoreInfo() {
    return this.keystoreInfo;
  }
}
