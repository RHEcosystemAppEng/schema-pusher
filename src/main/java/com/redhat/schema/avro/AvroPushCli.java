package com.redhat.schema.avro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import com.redhat.schema.AbstractCli;
import org.apache.avro.Schema.Parser;
import org.apache.kafka.clients.producer.KafkaProducer;
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
public final class AvroPushCli extends AbstractCli {
  private static final Logger LOG = Logger.getLogger(AvroPushCli.class.getName());

  private GenericApplicationContext context;

  public AvroPushCli(final GenericApplicationContext setContext) {
    this.context = setContext;
  }

  @Override
  protected void initialize() {
    context.registerBean(BeansConfig.Qualifiers.KAFKA_BOOTSTRAP_URL, String.class, getKafkaBootstrap());
    context.registerBean(BeansConfig.Qualifiers.SERVICE_REGISTRY_URL, String.class, getServiceRegistry());
    context.registerBean(BeansConfig.Qualifiers.NAMING_STRATEGY, String.class, getNamingStrategy().toString());
  }

  @Override
  public void run() {
    List<Path> filePaths;
    try {
      filePaths = getPathList(getDirectory());
    } catch (final IOException ioe) {
      LOG.severe(() -> String.format("failed to get files from directory: '%s'", getDirectory()));
      return;
    }

    try (var producer = context.getBean(KafkaProducer.class)) {
      @SuppressWarnings("unchecked")
      var pusher = new AvroPusher(producer);

      getTopicAggregators().parallelStream().forEach(ta -> {
        var parser = new Parser();
        filePaths.stream().forEach(path -> {
          try {
            pusher.push(parser, path, ta.getTopic()).get();
            logMessage(path.toFile().getName(), "Success.");
          } catch (final ExecutionException | InterruptedException | IOException exc) {
            if (exc instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
            logMessage(path.toFile().getName(), exc.getMessage());
          }
        });
      });
      producer.flush();
    }
  }

  void logMessage(final String fileName, final String message) {
    LOG.info(() -> String.format("%s%n%s%n", fileName, message));
  }
}
