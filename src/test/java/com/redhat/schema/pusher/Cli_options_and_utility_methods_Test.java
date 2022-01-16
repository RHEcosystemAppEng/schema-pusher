// editorconfig-checker-disable-max-line-length
package com.redhat.schema.pusher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenNoException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.ParameterException;

/** Test cases for the push cli abstraction, namely the cli option and properties. */
class Cli_options_and_utility_methods_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String FAKE_DIRECTORY = "/path/to/schemas/";
  private static final String FAKE_TRUSTSTORE_JKS_PATH = "/path/to/truststore.jks";
  private static final String FAKE_TRUSTSTORE_PASSWORD = "hideme123#@!";

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

  @CartesianTest
  void parsing_and_validating_all_possible_arguments_in_all_combinations_should_work_as_expected(
      @Values(strings = {"-b", "--bootstrap-url"}) final String bootstrapKey,
      @Values(strings = {"-r", "--registry-url"}) final String registryKey,
      @Values(strings = {"-n", "--naming-strategy"}) final String strategyKey,
      @Values(strings = {"-t", "--topic"}) final String topicKey,
      @Values(strings = {"-d", "--directory"}) final String directoryKey,
      @Values(strings = {"-j", "--truststore-jks-path"}) final String truststoreJksKey,
      @Values(strings = {"-p", "--truststore-password"}) final String truststorePasswordKey,
      @Enum final NamingStrategy strategy) {
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
                    directoryKey + "=" + FAKE_DIRECTORY,
                    truststoreJksKey + "=" + FAKE_TRUSTSTORE_JKS_PATH,
                    truststorePasswordKey + "=" + FAKE_TRUSTSTORE_PASSWORD));
    // and all the arguments should be aggregated
    then(sut.getKafkaBootstrap()).isEqualTo(FAKE_BOOTSTRAP);
    then(sut.getServiceRegistry()).isEqualTo(FAKE_REGISTRY);
    then(sut.getNamingStrategy()).isEqualByComparingTo(strategy);
    then(sut.getTopicAggregators()).singleElement().extracting("topic").isEqualTo(FAKE_TOPIC);
    then(sut.getDirectory()).isEqualTo(FAKE_DIRECTORY);
    // and the validation should pass
    thenNoException().isThrownBy(() -> sut.validate());
  }

  @ParameterizedTest
  @EnumSource(mode = Mode.EXCLUDE, names = "TOPIC_RECORD")
  void
      validating_multiple_topics_with_anything_besides_topic_record_naming_strategy_shoud_throw_an_exception(
          final NamingStrategy strategy) {
    // when parsing multiple topics with TOPIC or RECORD naming strategies
    cmd.parseArgs(
        "-b=" + FAKE_BOOTSTRAP,
        "-r=" + FAKE_REGISTRY,
        "-n=" + strategy.toString(),
        "-t=" + FAKE_TOPIC,
        "-t=anothertopic",
        "-d=" + FAKE_DIRECTORY);
    // then an exception should be thrown as this is not an acceptable configuration
    thenExceptionOfType(ParameterException.class)
        .isThrownBy(() -> sut.validate())
        .withMessage("For multiple topics, please use the default topic_record strategy.");
  }

  @Test
  void
      parsing_and_validating_using_the_default_naming_strategy_should_work_using_the_default_topic_record_strategy() {
    // when parsing with without specifying the naming strategy options
    // then no exceptions should be thrown
    assertThatNoException()
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-d=" + FAKE_DIRECTORY));
    // and the validation should pass
    thenNoException().isThrownBy(() -> sut.validate());
    // and the default used naming strategy should be TOPIC_RECORD
    then(sut.getNamingStrategy()).isEqualByComparingTo(NamingStrategy.TOPIC_RECORD);
  }

  @Test
  void parsing_without_specifying_the_kafka_bootstrap_url_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC, "-d=" + FAKE_DIRECTORY))
        .withMessage("Missing required option: '--bootstrap-url=<kafkaBootstrap>'");
  }

  @Test
  void parsing_without_specifying_the_registry_url_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-t=" + FAKE_TOPIC, "-d=" + FAKE_DIRECTORY))
        .withMessage("Missing required option: '--registry-url=<serviceRegistry>'");
  }

  @Test
  void parsing_without_specifying_at_least_one_topic_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-d=" + FAKE_DIRECTORY))
        .withMessage("Error: Missing required argument(s): (-t=<topic>)");
  }

  @Test
  void parsing_without_specifying_a_directory_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () -> cmd.parseArgs("-b=" + FAKE_BOOTSTRAP, "-r=" + FAKE_REGISTRY, "-t=" + FAKE_TOPIC))
        .withMessage("Missing required option: '--directory=<directory>'");
  }

  @Test
  void parsing_while_specifying_the_truststore_jks_path_but_not_the_password_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-d=" + FAKE_DIRECTORY,
                    "-j=" + FAKE_TRUSTSTORE_JKS_PATH))
        .withMessage("Error: Missing required argument(s): --truststore-password=<truststorePassword>");
  }

  @Test
  void parsing_while_specifying_the_truststore_password_but_not_the_jks_path_should_throw_an_exception() {
    assertThatExceptionOfType(MissingParameterException.class)
        .isThrownBy(
            () ->
                cmd.parseArgs(
                    "-b=" + FAKE_BOOTSTRAP,
                    "-r=" + FAKE_REGISTRY,
                    "-t=" + FAKE_TOPIC,
                    "-d=" + FAKE_DIRECTORY,
                    "-p=" + FAKE_TRUSTSTORE_PASSWORD))
        .withMessage("Error: Missing required argument(s): --truststore-jks-path=<truststoreJksPath>");
  }

  @Test
  void verify_the_getPathList_utility_method_with_a_test_folder_and_a_subfolder()
      throws IOException {
    // given the following target extensions
    var targetExtensions = List.of("json", "avsc", "avro");
    // when loading file from the test folder (including a subfolder)
    var filePaths = sut.getPathList(targetExtensions, "src/test/resources/com/redhat/schema/pusher/avro/schemas");
    // then only 3 suitable files should picked up
    then(filePaths).hasSize(3);
    // verify the file names
    var fileNames = filePaths.stream().map(Path::toFile).map(File::getName).toList();
    assertThat(fileNames)
        .containsExactlyInAnyOrder("test_schema.avsc", "test_schema_more.avro", "test_schema_too.json");
  }
}
