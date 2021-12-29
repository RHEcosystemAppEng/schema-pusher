package com.redhat.schema.avro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.redhat.schema.PushCli;
import com.redhat.schema.SchemaPusher;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(
  name = "avro_push",
  description = "Push AVRO schemas to Red Hat's Service Registry",
  mixinStandardHelpOptions = true,
  version = "0.0.1-SNAPSHOT"
)
public final class AvroPushCli extends PushCli {
  private static final Logger LOGGER = Logger.getLogger(AvroPushCli.class.getName());

  private final GenericApplicationContext context;
  private final SchemaPusher schemaPusher;

  public AvroPushCli(final GenericApplicationContext setContext, final SchemaPusher setSchemaPusher) {
    this.context = setContext;
    this.schemaPusher = setSchemaPusher;
  }

  @Override
  protected void initialize() {
    LOGGER.finer(() -> "registering the connection info as context beans");
    context.registerBean(BeansConfig.Qualifiers.KAFKA_BOOTSTRAP_URL, String.class, getKafkaBootstrap());
    context.registerBean(BeansConfig.Qualifiers.SERVICE_REGISTRY_URL, String.class, getServiceRegistry());
    context.registerBean(BeansConfig.Qualifiers.NAMING_STRATEGY, String.class, getNamingStrategy().toString());
  }

  @Override
  public void run() {
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
}
