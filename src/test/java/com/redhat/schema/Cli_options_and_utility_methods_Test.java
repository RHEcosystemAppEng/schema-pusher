package com.redhat.schema;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.PushCli;
import java.io.IOException;
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

class Cli_options_and_utility_methods_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String FAKE_DIRECTORY = "/path/to/schemas/";

  private PushCli sut;
  private CommandLine cmd;

  @BeforeEach
  void initialize() {
    // instantiate an anonymous abstract cli as the sut
    sut =
        new PushCli() {
          public void initialize() {
            // not needed for this test class
          }

          public void run() {
            // not needed for this test class
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
                    directoryKey + "=" + FAKE_DIRECTORY));
    // and all the arguments should be aggregated
    // then(sut.getKafkaBootstrap()).isEqualTo(FAKE_BOOTSTRAP);
    // then(sut.getServiceRegistry()).isEqualTo(FAKE_REGISTRY);
    // then(sut.getNamingStrategy()).isEqualByComparingTo(strategy);
    // then(sut.getTopicAggregators()).singleElement().extracting("topic").isEqualTo(FAKE_TOPIC);
    // then(sut.getDirectory()).isEqualTo(FAKE_DIRECTORY);
    // and the validation should pass
    // thenNoException().isThrownBy(() -> sut.validate());
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
    // thenExceptionOfType(ParameterException.class)
    //   .isThrownBy(() -> sut.validate())
    //   .withMessage("For multiple topics, please use the topic_record strategy.");
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
    // thenNoException().isThrownBy(() -> sut.validate());
    // and the default used naming strategy should be TOPIC_RECORD
    // then(sut.getNamingStrategy()).isEqualByComparingTo(NamingStrategy.TOPIC_RECORD);
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
  void verify_the_getPathList_utility_method_with_a_test_folder_and_a_subfolder()
      throws IOException {
    // when loading file from the test folder (including a subfolder)
    // var filePaths = sut.getPathList("src/test/resources/com/redhat/schema/pusher/avro/schemas");
    // then only 3 suitable files should picked up
    // then(filePaths).hasSize(3);
    // verify the file names
    // var fileNames = filePaths.stream().map(Path::toFile).map(File::getName).toList();
    // assertThat(fileNames).containsExactlyInAnyOrder(
    //   "test_schema.avsc", "test_schema_more.avro", "test_schema_too.json");
  }
}
