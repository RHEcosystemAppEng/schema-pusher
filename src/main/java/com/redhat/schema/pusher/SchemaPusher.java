package com.redhat.schema.pusher;

import java.nio.file.Path;
import java.util.List;

/** Interface for contracting the scheme pusher implementations. */
public interface SchemaPusher {
  /**
   * Push a list of schemas to push to for topic in a list of topics.
   *
   * @param topics a {@link List} of {@link String} topic to push the schemas with.
   * @param schemas a {@link List} of {@link Path} of schema files to push.
   * @return an integer return code
   */
  ReturnCode push(List<String> topics, List<Path> schemas);
}
