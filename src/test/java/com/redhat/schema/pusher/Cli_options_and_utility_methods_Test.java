// editorconfig-checker-disable-max-line-length
package com.redhat.schema.pusher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;

/** Test cases for the push cli abstraction, namely the cli option and properties. */
class Cli_options_and_utility_methods_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String FAKE_SCHEMA_FILE = "/path/to/schema/file.json";
  private static final String FAKE_TRUSTSTORE_FILE = "/path/to/truststore.p12";
  private static final String FAKE_TRUSTSTORE_PASSWORD = "hideme123#@!";
  private static final String FAKE_KEYSTORE_FILE = "/path/to/keystore.p12";
  private static final String FAKE_KEYSTORE_PASSWORD = "hidemetoo123#@!";
  private static final String FAKE_CUSTOM_PROPERTY_KEY = "custom key";
  private static final String FAKE_CUSTOM_PROPERTY_VALUE = "custom value";

  private PushCli sut;
  private CommandLine cmd;

  @BeforeEach
  void initialize() {
    // instantiate an anonymous abstract cli as the sut
    sut =
        new PushCli() {
          public Integer call() {
            // not needed for this test class
            return 0;
          }
        };
    // load the instance as a command line
    cmd = new CommandLine(sut);
  }

  @ParameterizedTest
  @MethodSource
  void parsing_all_possible_arguments_combined_should_not_throw_an_exception(
      final String bootstrapKey,
      final String registryKey,
      final String strategyKey,
      final String topicKey,
      final String schemaPathKey,
      final String propertyKey,
      final String propertyValue,
      final String truststoreFileKey,
      final String truststorePasswordKey,
      final String keystoreFileKey,
      final String keystorePasswordKey,
      final NamingStrategy strategy) {
    // when parsing the command line args with all the possible options
    // then no exception should be thrown
    assertThatNoException()
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    bootstrapKey + "=" + FAKE_BOOTSTRAP,
                    registryKey + "=" + FAKE_REGISTRY,
                    strategyKey + "=" + strategy.toString(),
                    topicKey + "=" + FAKE_TOPIC,
                    schemaPathKey + "=" + FAKE_SCHEMA_FILE,
                    propertyKey + "=" + FAKE_CUSTOM_PROPERTY_KEY,
                    propertyValue + "=" + FAKE_CUSTOM_PROPERTY_VALUE,
                    truststoreFileKey + "=" + FAKE_TRUSTSTORE_FILE,
                    truststorePasswordKey + "=" + FAKE_TRUSTSTORE_PASSWORD,
                    keystoreFileKey + "=" + FAKE_KEYSTORE_FILE,
                    keystorePasswordKey + "=" + FAKE_KEYSTORE_PASSWORD));
    // and all the arguments should be aggregated
    assertThat(sut.getKafkaBootstrap()).isEqualTo(FAKE_BOOTSTRAP);
    assertThat(sut.getServiceRegistry()).isEqualTo(FAKE_REGISTRY);
    assertThat(sut.getNamingStrategy()).isEqualByComparingTo(strategy);
    assertThat(sut.getTopicSchemaAggregators()).singleElement()
        .hasFieldOrPropertyWithValue("topic", FAKE_TOPIC)
        .hasFieldOrPropertyWithValue("schemaPath", Paths.get(FAKE_SCHEMA_FILE));
  }

  static Stream<Arguments> parsing_all_possible_arguments_combined_should_not_throw_an_exception() {
    return Stream.of(
      arguments(
        "--bootstrap-url",
        "--registry-url",
        "--naming-strategy",
        "--topic",
        "--schema-path",
        "--property-key",
        "--property-value",
        "--truststore-file",
        "--truststore-password",
        "--keystore-file",
        "--keystore-password",
        NamingStrategy.TOPIC_RECORD),
      arguments("-b", "-r", "-n", "-t", "-s", "--pk", "--pv", "--tf", "--tp", "--kf", "--kp", NamingStrategy.TOPIC_RECORD));
  }

  @Test
  void
      parsing_using_the_default_naming_strategy_should_work_using_the_default_topic_record_strategy() {
    // when parsing with without specifying the naming strategy options
    // then no exceptions should be thrown
    assertThatNoException()
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE));
    // and the default used naming strategy should be TOPIC_RECORD
    assertThat(sut.getNamingStrategy()).isEqualByComparingTo(NamingStrategy.TOPIC_RECORD);
  }

  @Test
  void parsing_without_specifying_the_kafka_bootstrap_url_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC, "-s=" + FAKE_SCHEMA_FILE))
        .withMessage("Missing required option: '--bootstrap-url=<kafkaBootstrap>'");
  }

  @Test
  void parsing_without_specifying_the_registry_url_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-t=" + FAKE_TOPIC, "-s=" + FAKE_SCHEMA_FILE))
        .withMessage("Missing required option: '--registry-url=<serviceRegistry>'");
  }

  @Test
  void parsing_without_specifying_a_topic_and_a_schema_path_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY))
        .withMessage("Error: Missing required argument(s): (-t=<topic> -s=<schemaPath>)");
  }

  @Test
  void parsing_with_a_topic_without_specifying_a_schema_path_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC))
        .withMessage("Error: Missing required argument(s): --schema-path=<schemaPath>");
  }

  @Test
  void parsing_with_a_schema_path_without_specifying_a_topic_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-s=" + FAKE_SCHEMA_FILE))
        .withMessage("Error: Missing required argument(s): --topic=<topic>");
  }

  @Test
  void parsing_with_two_topics_and_one_a_schema_path_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs(
              "-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC, "-s=" + FAKE_SCHEMA_FILE, "-t=anothertopic"))
        .withMessage("Error: Missing required argument(s): --schema-path=<schemaPath>");
  }

  @Test
  void parsing_with_two_schema_paths_and_one_topic_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs(
              "-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC, "-s=" + FAKE_SCHEMA_FILE, "-s=/another/schema.json"))
        .withMessage("Error: Missing required argument(s): --topic=<topic>");
  }

  @Test
  void parsing_while_specifying_the_truststore_file_but_not_the_password_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--tf=" + FAKE_TRUSTSTORE_FILE))
        .withMessage("Error: Missing required argument(s): --truststore-password=<truststorePassword>");
  }

  @Test
  void parsing_while_specifying_the_truststore_password_but_not_the_file_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--tp=" + FAKE_TRUSTSTORE_PASSWORD))
        .withMessage("Error: Missing required argument(s): --truststore-file=<truststoreFile>");
  }

  @Test
  void parsing_while_specifying_the_keystore_file_but_not_the_password_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--kf=" + FAKE_KEYSTORE_FILE))
        .withMessage("Error: Missing required argument(s): --keystore-password=<keystorePassword>");
  }

  @Test
  void parsing_while_specifying_the_keystore_password_but_not_the_file_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--kp=" + FAKE_KEYSTORE_PASSWORD))
        .withMessage("Error: Missing required argument(s): --keystore-file=<keystoreFile>");
  }

  @Test
  void parsing_while_specifying_the_property_key_but_not_the_value_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--pk=" + FAKE_CUSTOM_PROPERTY_KEY))
        .withMessage("Error: Missing required argument(s): --property-value=<propertyValue>");
  }

  @Test
  void parsing_while_specifying_the_property_value_but_not_the_key_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-s=" + FAKE_SCHEMA_FILE,
                    "--pv=" + FAKE_CUSTOM_PROPERTY_VALUE))
        .withMessage("Error: Missing required argument(s): --property-key=<propertyKey>");
  }
}
