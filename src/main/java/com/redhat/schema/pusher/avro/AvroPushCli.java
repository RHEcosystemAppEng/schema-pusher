package com.redhat.schema.pusher.avro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.SchemaPusher;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;

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

  public AvroPushCli(final GenericApplicationContext setContext) {
    this.context = setContext;
  }

  @Override
  public void run() {
    validate();
    var schemaPusher = context.getBean(
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

  private void validate() {
    if (getTopicAggregators().size() > 1 && !getNamingStrategy().equals(NamingStrategy.TOPIC_RECORD)) {
      throw new ParameterException(
        getSpec().commandLine(), "For multiple topics, please use the topic_record strategy.");
    }
  }
}
