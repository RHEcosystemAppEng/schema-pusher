package com.redhat.schema.pusher.avro;

import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.SchemaPusher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;

/**
 * AVRO implementation of the {@link PushCli}, specifies the {@code picocli.CommandLine.Command}
 * configuration and provides the execution of the command.
 */
@Component
@Command(
  name = "avro_push",
  description = "Push AVRO schemas to Red Hat's Service Registry",
  mixinStandardHelpOptions = true,
  version = "0.0.1-SNAPSHOT")
public final class AvroPushCli extends PushCli {
  private static final Logger LOGGER = Logger.getLogger(AvroPushCli.class.getName());

  private final ApplicationContext context;

  /**
   * Constructor that takes the injected DI context.
   *
   * @param setContext an instance of {@code org.springframework.context.ApplicationContext}.
   */
  public AvroPushCli(final ApplicationContext setContext) {
    this.context = setContext;
  }

  /**
   * Grabs the {@link SchemaPusher} implementation from the DI context, an invoke it using the
   * user's specified schema files and topic list.
   */
  @Override
  public void run() {
    validate();
    var schemaPusher =
        context.getBean(
            SchemaPusher.class, getKafkaBootstrap(), getServiceRegistry(), getNamingStrategy());
    List<Path> schemas;
    try {
      schemas = getPathList(getDirectory());
    } catch (final IOException ioe) {
      LOGGER.log(
        Level.SEVERE, ioe, () -> String.format("failed to get schema files from directory: '%s'", getDirectory()));
      return;
    }
    var topics = getTopicAggregators().stream().map(TopicAggregator::getTopic).toList();
    schemaPusher.push(topics, schemas);
  }

  /** Use for validating the user specified arguments. */
  private void validate() {
    if (getTopicAggregators().size() > 1
        && !getNamingStrategy().equals(NamingStrategy.TOPIC_RECORD)) {
      throw new ParameterException(
        getSpec().commandLine(), "For multiple topics, please use the topic_record strategy.");
    }
  }
}
