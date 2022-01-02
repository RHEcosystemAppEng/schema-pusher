package com.redhat.schema.pusher.avro;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.SchemaPusher;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

@ExtendWith(MockitoExtension.class)
class Avro_push_cli_implemenetation_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String DIRECTORY = "com/redhat/schema/pusher/avro/schemas/subfolder";
  private static final NamingStrategy FAKE_NAMING_STRATEGY = NamingStrategy.TOPIC_RECORD;

  @Mock private ApplicationContext mockContext;
  @InjectMocks private AvroPushCli sut;

  @BeforeEach
  void initialize() throws URISyntaxException {
    var directoryAbs = Paths.get(getClass().getClassLoader().getResource(DIRECTORY).toURI());
    // parse the command line arguments for the sut
    new CommandLine(sut).parseArgs(
        "-b=" + FAKE_BOOTSTRAP,
        "-r=" + FAKE_REGISTRY,
        "-n=" + FAKE_NAMING_STRATEGY.toString(),
        "-t=" + FAKE_TOPIC,
        "-d=" + directoryAbs.toString()
    );
  }

  @Test
  void executing_the_cli_implementation_should_invoke_the_schmea_pusher_implementation(
      @Mock final SchemaPusher mockSchemaPusher) throws URISyntaxException {
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // given the target directory has only one file
    var expectedFile = Paths.get(getClass().getClassLoader().getResource(DIRECTORY + "/test_schema_too.json").toURI());
    // given the mocked di context will return  the mock schema pusher per the arguments
    given(mockContext.getBean(
      SchemaPusher.class, FAKE_BOOTSTRAP, FAKE_REGISTRY, FAKE_NAMING_STRATEGY)).willReturn(mockSchemaPusher);
    // when the sut executes
    sut.run();
    // then the mock pusher should be pushed with the fake topic and the testing schema file path
    then(mockSchemaPusher).should().push(eq(List.of(FAKE_TOPIC)), eq(List.of(expectedFile)));
  }
}
