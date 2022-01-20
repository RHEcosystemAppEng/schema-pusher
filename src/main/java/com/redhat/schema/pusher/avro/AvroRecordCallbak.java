package com.redhat.schema.pusher.avro;

import static java.util.Objects.nonNull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AvroRecordCallbak implements Callback {
  private final Logger pusherLogger;
  private final String fileName;
  private final String destTopic;

  public AvroRecordCallbak(final Logger setPusherLogger, final String setFilename, final String setDestTopic) {
    this.pusherLogger = setPusherLogger;
    this.fileName = setFilename;
    this.destTopic = setDestTopic;
  }

  @Override
  public void onCompletion(final RecordMetadata metadata, final Exception exception) {
    if(nonNull(exception)) {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      pusherLogger.log(
        Level.SEVERE,
        exception,
        () -> String.format( "failed to push '%s' to topic '%s'", fileName, destTopic));
    } else {
      pusherLogger.info(
        () -> String.format("successfully pushed '%s' to topic '%s'", fileName, destTopic));
    }
  }
}
