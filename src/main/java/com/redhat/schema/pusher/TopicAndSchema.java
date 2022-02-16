package com.redhat.schema.pusher;

import java.nio.file.Path;

/** A record for aggregating topic {@link String} and schema {@link Path} pairs. */
public record TopicAndSchema(String topic, Path schema){}
