package com.redhat.schema.pusher;

import com.redhat.schema.pusher.avro.AvroBeansConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

public final class MainApp {
  public static void main(final String... args) {
    try (var context = new AnnotationConfigApplicationContext(AvroBeansConfig.class)) {
      var cliImpl = context.getBean(PushCli.class);
      var cli = new CommandLine(cliImpl);
      cli.setCaseInsensitiveEnumValuesAllowed(true);
      cli.execute(args);
    }
  }
}
