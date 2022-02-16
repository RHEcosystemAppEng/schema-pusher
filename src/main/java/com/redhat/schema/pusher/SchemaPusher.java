package com.redhat.schema.pusher;

import java.util.List;

/** Interface for contracting the schema pusher implementations. */
public interface SchemaPusher {
  /**
   * Push a list of schemas to push to for topic in a list of topics.
   *
   * @param topicSchemaRecords a {@link List} of {@link TopicAndSchema} records.
   * @return a {@link ReturnCode} instance.
   */
  ReturnCode push(List<TopicAndSchema> topicSchemaRecords);
}
