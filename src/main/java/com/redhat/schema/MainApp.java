package com.redhat.schema;

import com.redhat.schema.avro.BeansConfig;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

public final class MainApp {
  public static void main(final String... args) {
    try (var context = new AnnotationConfigApplicationContext(BeansConfig.class)) {
      var cliImpl = context.getBean(AbstractCli.class);
      var cli = new CommandLine(cliImpl);
      cli.setExecutionStrategy(cliImpl::executionStrategy);
      cli.setCaseInsensitiveEnumValuesAllowed(true);
      cli.execute(args);
    }
  }
}
