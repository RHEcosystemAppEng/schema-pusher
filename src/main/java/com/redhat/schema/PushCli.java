package com.redhat.schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

public abstract class PushCli implements Runnable {
  @Option(
    names = {"-b", "--bootstrap-url"},
    description = "The url for Kafka's bootstrap server.",
    required = true
  )
  private String kafkaBootstrap;

  @Option(
    names = {"-r", "--registry-url"},
    description = "The url for Red Hat's service registry.",
    required = true
  )
  private String serviceRegistry;

  @Option(
    names = {"-n", "--naming-strategy"},
    description = "The subject naming strategy.",
    defaultValue = "TOPIC_RECORD"
  )
  private NamingStrategy namingStrategy;

  @ArgGroup(exclusive = false, multiplicity = "1..*")
  private List<TopicAggregator> topicAggregators;

  @Option(
    names = {"-d", "--directory"},
    description = "The path of the directory containing the schema files.",
    required = true
  )
  private String directory;

  @Spec
  private CommandSpec spec;

  protected static class TopicAggregator {
    @Option(
      names = {"-t", "--topic"},
      description = "The topic to produce the message too, repeatable.",
      required = true
    )
    private String topic;

    public String getTopic() {
      return this.topic;
    }
  }

  int executionStrategy(final ParseResult parseResult) {
    initialize();
    validate();
    return new CommandLine.RunLast().execute(parseResult);
  }

  protected abstract void initialize();

  protected void validate() {
    if (getTopicAggregators().size() > 1 && !getNamingStrategy().equals(NamingStrategy.TOPIC_RECORD)) {
      throw new ParameterException(
        getSpec().commandLine(), "For multiple topics, please use the topic_record strategy.");
    }
  }

  protected final List<Path> getPathList(final String directory) throws IOException {
    try (var walkStream = Files.walk(Paths.get(directory))) {
      return walkStream.filter(Files::isRegularFile).toList();
    }
  }

  protected String getKafkaBootstrap() {
    return this.kafkaBootstrap;
  }

  protected String getServiceRegistry() {
    return this.serviceRegistry;
  }

  protected NamingStrategy getNamingStrategy() {
    return this.namingStrategy;
  }

  protected List<TopicAggregator> getTopicAggregators() {
    return this.topicAggregators;
  }

  protected String getDirectory() {
    return this.directory;
  }

  protected CommandSpec getSpec() {
    return this.spec;
  }
}
