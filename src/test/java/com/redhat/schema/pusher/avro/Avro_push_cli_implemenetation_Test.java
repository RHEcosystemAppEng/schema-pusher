package com.redhat.schema.pusher.avro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.redhat.schema.pusher.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

/** Test cases for the AVRO push cli implementation. */
@ExtendWith(MockitoExtension.class)
class Avro_push_cli_implemenetation_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String TESTING_SCHEMA =
      "com/redhat/schema/pusher/avro/schemas/test_schema2.avsc";
  private static final NamingStrategy FAKE_NAMING_STRATEGY = NamingStrategy.TOPIC_RECORD;

  @Mock private ApplicationContext mockContext;
  @InjectMocks private AvroPushCli sut;

  @BeforeEach
  void initialize() throws URISyntaxException {
    // parse the command line arguments for the sut
    new CommandLine(sut)
        .parseArgs(
            "-b=" + FAKE_BOOTSTRAP,
            "-r=" + FAKE_REGISTRY,
            "-n=" + FAKE_NAMING_STRATEGY.toString(),
            "-t=" + FAKE_TOPIC,
            "-s=" + TESTING_SCHEMA);
  }

  @Test
  void executing_the_cli_implementation_should_invoke_the_schmea_pusher_implementation(
      @Mock final SchemaPusher mockSchemaPusher) throws URISyntaxException {
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // the expected TopicAndSchema record
    var expectedRecord = new TopicAndSchema(FAKE_TOPIC, Paths.get(TESTING_SCHEMA));
    // given the mocked di context will return  the mock schema pusher per the arguments
    given(mockContext.getBean(eq(SchemaPusher.class), any(PushCli.class)))
        .willReturn(mockSchemaPusher);
    // given the mocked schema pusher will return a success code for the fake topic and testing
    // schema file path
    given(mockSchemaPusher.push(List.of(expectedRecord))).willReturn(ReturnCode.SUCCESS);
    // when the sut executes, then the stubbed return success code should be success
    assertThat(sut.call()).isEqualTo(ReturnCode.SUCCESS.code());
  }
}
