package com.redhat.schema.avro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class Avro_Push_Cli_Test {
  private AvroPushCli sut;
  private CommandLine cmd;

  @BeforeEach
  void initialize() {
    sut = new AvroPushCli();
    cmd = new CommandLine(sut);
  }

  @Test
  void test_run() {
    assertThat(true).isTrue();
  }
}
