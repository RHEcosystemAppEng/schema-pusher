package com.redhat.schema.pusher.avro;

import com.redhat.schema.pusher.ManifestVersionProvider;
import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.SchemaPusher;
import com.redhat.schema.pusher.TopicAndSchema;
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
    description = "Push schemas to Red Hat's Service Registry",
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider.class)
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
  public Integer call() {
    LOGGER.info("starting");
    LOGGER.info("creating topic and schema pair records");
    var topicsAndSchemaRecords = getTopicSchemaAggregators().stream()
        .map(a -> new TopicAndSchema(a.getTopic(), a.getSchemaPath())).toList();
    LOGGER.info("loading schema pusher");
    var schemaPusher = context.getBean(SchemaPusher.class, this);
    LOGGER.info("starting push");
    var retCode = schemaPusher.push(topicsAndSchemaRecords);
    LOGGER.info("done");
    return retCode.code();
  }
}
