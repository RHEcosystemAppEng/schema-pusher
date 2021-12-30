package com.redhat.schema.pusher;

import java.nio.file.Path;
import java.util.List;

public interface SchemaPusher {
  void push(List<String> topics, List<Path> schemas);
}
