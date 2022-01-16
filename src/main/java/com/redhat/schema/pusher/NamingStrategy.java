package com.redhat.schema.pusher;

import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;

/** Enum for relaying the subject naming strategy. */
// CHECKSTYLE:OFF: IllegalIdentifierName
public enum NamingStrategy {
  /**
   * Use {@code io.confluent.kafka.serializers.subject.TopicNameStrategy},
   * e.g. {@code exampletopic-value}.
   */
  TOPIC(TopicNameStrategy.class),
  /**
   * Use {@code io.confluent.kafka.serializers.subject.RecordNameStrategy},
   * e.g. {@code com.example.RecordName}.
   */
  RECORD(RecordNameStrategy.class),
  /**
   * Use {@code io.confluent.kafka.serializers.subject.TopicRecordNameStrategy},
   * e.g. {@code exampletopic-com.example.RecordName}.
   */
  TOPIC_RECORD(TopicRecordNameStrategy.class);

  private final Class<?> strategy;

  NamingStrategy(final Class<? extends SubjectNameStrategy> setStrategy) {
    this.strategy = setStrategy;
  }

  /**
   * Get the relayed strategy class.
   *
   * @return a Class extending
   *     {@code io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy}.
   */
  public Class<?> getStrategy() {
    return this.strategy;
  }
}
