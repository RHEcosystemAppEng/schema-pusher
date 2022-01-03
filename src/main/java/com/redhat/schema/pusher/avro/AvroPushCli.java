package com.redhat.schema.pusher.avro;

import static com.redhat.schema.pusher.ReturnCode.DIRECTORY_ERROR;

import com.redhat.schema.pusher.ManifestVersionProvider;
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

/**
 * AVRO implementation of the {@link PushCli}, specifies the {@code picocli.CommandLine.Command}
 * configuration and provides the execution of the command.
 */
@Component
@Command(
  name = "avro_push",
  description = "Push AVRO schemas to Red Hat's Service Registry",
  mixinStandardHelpOptions = true,
  versionProvider = ManifestVersionProvider.class)
public final class AvroPushCli extends PushCli {
  private static final Logger LOGGER = Logger.getLogger(AvroPushCli.class.getName());
  private static final List<String> SUPPORTED_EXTENSIONS = List.of("json", "avsc", "avro");

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
  public Integer call() {
    validate();
    LOGGER.info("starting");
    var schemaPusher =
        context.getBean(
            SchemaPusher.class, getKafkaBootstrap(), getServiceRegistry(), getNamingStrategy());
    LOGGER.info("loading schemas");
    List<Path> schemas;
    try {
      schemas = getPathList(SUPPORTED_EXTENSIONS, getDirectory());
    } catch (final IOException ioe) {
      LOGGER.log(
        Level.SEVERE, ioe, () -> String.format("failed to get schema files from directory '%s'", getDirectory()));
      return DIRECTORY_ERROR.code();
    }
    LOGGER.info("loading topics");
    var topics = getTopicAggregators().stream().map(TopicAggregator::getTopic).toList();
    var retCode = schemaPusher.push(topics, schemas);
    LOGGER.info("done");
    return retCode.code();
  }
}
