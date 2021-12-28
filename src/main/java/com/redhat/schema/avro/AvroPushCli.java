package com.redhat.schema.avro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import com.redhat.schema.AbstractCli;
import org.apache.avro.Schema.Parser;
import picocli.CommandLine.Command;

@Command(
  name = "avro_push",
  description = "Push AVRO schemas to Red Hat's Service Registry",
  mixinStandardHelpOptions = true,
  version = "0.0.1-SNAPSHOT"
)
public final class AvroPushCli extends AbstractCli {
  private static final Logger LOG = Logger.getLogger(AvroPushCli.class.getName());

  @Override
  public void run() {
    validate();
    var propsAggregator = new AvroProperties(getKafkaBootstrap(), getServiceRegistry(), getNamingStrategy());
    List<Path> filePaths;
    try {
      filePaths = getFilesPath(getDirectory());
    } catch (final IOException ioe) {
      LOG.severe(() -> String.format("failed to get files from directory: '%s'", getDirectory()));
      return;
    }

    try (var producer = new AvroProducer(propsAggregator)) {
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
