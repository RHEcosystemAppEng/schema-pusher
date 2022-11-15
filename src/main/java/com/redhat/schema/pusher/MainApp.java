package com.redhat.schema.pusher;

import static java.util.Objects.isNull;

import com.redhat.schema.pusher.avro.AvroBeansConfig;
import java.io.IOException;
import java.util.logging.LogManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

/** The main starting point of the application. */
public final class MainApp {
  private static final String DEFAULT_LOG_CONF =
      "com/redhat/schema/pusher/logging-default.properties";

  private MainApp() {
    // a utility class needs no constructor
  }

  /**
   * Creates a DI context, grabs the {@link PushCli} implementation and loads a {@code
   * picocli.CommandLine} instance.
   *
   * @param args the CLI argument pairs for parsing.
   */
  public static void main(final String... args) {
    // if no logging configuration was set by the user, load the default configuration
    if (isNull(System.getProperty("java.util.logging.config.file"))) {
      try (var is = MainApp.class.getClassLoader().getResourceAsStream(DEFAULT_LOG_CONF)) {
        LogManager.getLogManager().readConfiguration(is);
      } catch (final IOException ioe) {
        LogManager.getLogManager().reset();
      }
    }
    // start di context, load the command line and execute it
    try (var context = new AnnotationConfigApplicationContext(AvroBeansConfig.class)) {
      var cliImpl = context.getBean(PushCli.class);
      var cli = new CommandLine(cliImpl);
      cli.setCaseInsensitiveEnumValuesAllowed(true);
      System.exit(cli.execute(args));
    }
  }
}
