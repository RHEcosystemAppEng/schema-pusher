package com.redhat.schema.pusher.avro;

import com.redhat.schema.pusher.NamingStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class Avro_push_cli_implemenetation_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry";
  private static final String FAKE_TOPIC = "faketopic";
  private static final String FAKE_DIRECTORY = "/path/to/schemas/";
  private static final NamingStrategy FAKE_NAMING_STRATEGY = NamingStrategy.TOPIC_RECORD;

  @Mock private ApplicationContext mockContext;
  @InjectMocks private AvroPushCli sut;

  @BeforeEach
  void initialize() {

  }
}
