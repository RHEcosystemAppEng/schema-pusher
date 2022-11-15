package com.redhat.schema.pusher.avro;

import java.io.Closeable;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface Producer<K, V> extends Closeable {
    void send(ProducerRecord<K, V> record);
}
