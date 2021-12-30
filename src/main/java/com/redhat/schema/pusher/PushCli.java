package com.redhat.schema.pusher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Command line specification and utility methods, CLI implementations should extend this class. */
public abstract class PushCli implements Runnable {
  @Spec private CommandSpec spec;

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

  @Option(
    names = {"-d", "--directory"},
    description = "The path of the directory containing the schema files.",
    required = true)
  private String directory;

  @ArgGroup(exclusive = false, multiplicity = "1..*")
  private List<TopicAggregator> topicAggregators;

  /** Use for aggregating topics specified by the user. */
  protected static class TopicAggregator {
    @Option(
      names = {"-t", "--topic"},
      description = "The topic to produce the message too, repeatable.",
      required = true)
    private String topic;

    public String getTopic() {
      return this.topic;
    }
  }

  /* *************** *
   * Utility Methods *
   * *************** */

  /**
   * Get a recursive list of files from a directory with filtering by file extensions.
   *
   * @param extensions a list of extensions to filter by.
   * @param directory the directory to look in.
   * @return a {@link List} of {@link Path}.
   * @throws IOException when failed to get the directory or files.
   */
  protected final List<Path> getPathList(
      final List<String> extensions, final String directory) throws IOException {
    final Predicate<Path> extensionFilter =
        p -> {
          var file = p.toFile().getName();
          var idx = file.lastIndexOf(".");
          if (idx < 0) {
            return false;
          }
          var ext = file.substring(idx + 1);
          return extensions.contains(ext);
        };
    try (var walkStream = Files.walk(Paths.get(directory))) {
      return walkStream.filter(Files::isRegularFile).filter(extensionFilter).toList();
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
  protected String getKafkaBootstrap() {
    return this.kafkaBootstrap;
  }

  /**
   * Get Red Hat's service registry url as specified by the user.
   *
   * @return a {@link String} url.
   */
  protected String getServiceRegistry() {
    return this.serviceRegistry;
  }

  /**
   * Get the naming strategy as specified by the user.
   *
   * @return a {@link NamingStrategy} member.
   */
  protected NamingStrategy getNamingStrategy() {
    return this.namingStrategy;
  }

  /**
   * Get the list of topics as specified by the user.
   *
   * @return a {@link List} of {@link TopicAggregator} instances.
   */
  protected List<TopicAggregator> getTopicAggregators() {
    return this.topicAggregators;
  }

  /**
   * Get the schema files directory for scaning as specified by the user.
   *
   * @return a {@link String} directory path.
   */
  protected String getDirectory() {
    return this.directory;
  }

  /**
   * Get the spec of the command line.
   *
   * @return the {@code picocli.CommandLine.Model.CommandSpec} injected instance.
   */
  protected CommandSpec getSpec() {
    return this.spec;
  }
}
