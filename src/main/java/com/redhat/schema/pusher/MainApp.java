package com.redhat.schema.pusher;

import com.redhat.schema.pusher.avro.AvroBeansConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

/** The main starting point of the application. */
public final class MainApp {
  /**
   * Creates a DI context, grabs the {@link PushCli} implementation and loads a
   * {@code picocli.CommandLine} instance.
   *
   * @param args the CLI argument pairs for parsing.
   */
  public static void main(final String... args) {
    try (var context = new AnnotationConfigApplicationContext(AvroBeansConfig.class)) {
      var cliImpl = context.getBean(PushCli.class);
      var cli = new CommandLine(cliImpl);
      cli.setCaseInsensitiveEnumValuesAllowed(true);
      cli.execute(args);
    }
  }
}
