package com.redhat.schema.avro;

import com.redhat.schema.SchemaPusher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public final class AvroSchemaPusher implements SchemaPusher {
  private static final Logger LOGGER = Logger.getLogger(AvroSchemaPusher.class.getName());

  private final ApplicationContext context;

  public AvroSchemaPusher(final ApplicationContext setContext) {
    this.context = setContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void push(final List<String> topics, final List<Path> schemas) {
    try (var producer = context.getBean(KafkaProducer.class)) {
      topics.parallelStream().forEach(topic -> {
        var parser = new Parser();
        schemas.stream().forEach(path -> {
          try {
            var schema = parser.parse(Files.newInputStream(path));
            var avroRec = new GenericData.Record(schema);
            var prodRec = new ProducerRecord<String, IndexedRecord>(topic, avroRec);
            producer.send(prodRec).get();
          } catch (final  ExecutionException | InterruptedException | IOException exc) {
            if (exc instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
            LOGGER.log(
              Level.SEVERE, exc, () -> String.format("failed to push schema %s to topic %s", path.toFile().getName(), topic));
          }
        });
      });
      producer.flush();
    }
  }
}
