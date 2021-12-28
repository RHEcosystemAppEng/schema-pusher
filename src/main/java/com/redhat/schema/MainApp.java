package com.redhat.schema;

import com.redhat.schema.avro.AvroPushCli;
import picocli.CommandLine;

public final class MainApp {
  public static void main(final String... args) {
    var cli = new CommandLine(new AvroPushCli());
    cli.setCaseInsensitiveEnumValuesAllowed(true);
    cli.execute(args);
  }
}
