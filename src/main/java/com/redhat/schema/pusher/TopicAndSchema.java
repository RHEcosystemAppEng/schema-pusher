package com.redhat.schema.pusher;

import java.nio.file.Path;

/**
 * A record for aggregating topic {@link String} and schema {@link Path} pairs.
 *
 * @param topic the topic for the record.
 * @param schema the path for the schema file.
 */
public record TopicAndSchema(String topic, Path schema){}
