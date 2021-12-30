package com.redhat.schema.pusher.avro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.redhat.schema.pusher.NamingStrategy;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class Avro_schema_pusher_implementation_Test {
  private static final String FAKE_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry/";
  private static final NamingStrategy FAKE_NAMING_STRATEGY = NamingStrategy.TOPIC_RECORD;
  private static final String FAKE_TOPIC1 = "faketopic";
  private static final String FAKE_TOPIC2 = "anotherfaketopic";

  @Captor ArgumentCaptor<ProducerRecord<String, IndexedRecord>> prodRecCaptore;
  @Mock private KafkaProducer<String, IndexedRecord> mockProducer;
  @Mock private ApplicationContext mockContext;
  private AvroSchemaPusher sut;

  @BeforeEach
  void initialize() {
    // instantiate the sut with the fake info
    sut = new AvroSchemaPusher(FAKE_BOOTSTRAP, FAKE_REGISTRY, FAKE_NAMING_STRATEGY);
    // stub the private di context
    setField(sut, "context", mockContext);
  }

  @Test
  @SuppressWarnings("unchecked")
  void a_failed_push_should_not_throw_an_exception(@Mock final Future<RecordMetadata> mockFuture)
      throws URISyntaxException, InterruptedException, ExecutionException {
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // short-circuit the future's get method to throw an exception
    given(mockFuture.get()).willThrow(new InterruptedException());
    // stub the producer with a mocked future for any send methd invocation
    given(mockProducer.send(any(ProducerRecord.class))).willReturn(mockFuture);
    // given the di context will return the mocked producer as bean per the properties match
    given(mockContext.getBean(eq(KafkaProducer.class), argThat(propertiesMatcher))).willReturn(mockProducer);
    // given the following non existing schema file
    var testSchema = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema.avsc");
    // when invoking the push method with it no exceptions should be thrown
    assertThatNoException().isThrownBy(() -> sut.push(List.of(FAKE_TOPIC1), List.of(testSchema)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void pushing_two_files_and_two_topics_should_result_in_four_producer_records_sent() throws URISyntaxException {
    // stub the producer with a mocked future for any send methd invocation
    given(mockProducer.send(any(ProducerRecord.class))).willReturn(mock(Future.class));
    // given the di context will return the mocked producer as bean per the properties match
    given(mockContext.getBean(eq(KafkaProducer.class), argThat(propertiesMatcher))).willReturn(mockProducer);
    // given the following two schema test files
    var testSchema1 = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema.avsc");
    var testSchema2 = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema_more.avro");
    // when invoking the push method with two topics and two schema files
    sut.push(List.of(FAKE_TOPIC1, FAKE_TOPIC2), List.of(testSchema1, testSchema2));
    // then producer should be invoked in order
    var mockProdOrder = inOrder(mockProducer);
    // four invocations of the send method, one per schema+topic
    then(mockProducer).should(mockProdOrder, times(4)).send(prodRecCaptore.capture());
    // and one invocation for flushing the producer cache
    then(mockProducer).should(mockProdOrder).flush();
    // verify each schema was sent to each topic (schema names are from in the schema files)
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC1, "TestingSchema"))
        .hasSize(1);
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC1, "TestingSchemaMore"))
        .hasSize(1);
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC2, "TestingSchema"))
        .hasSize(1);
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC2, "TestingSchemaMore"))
        .hasSize(1);
  }

  private ArgumentMatcher<Properties> propertiesMatcher =
      p -> FAKE_BOOTSTRAP.equals(p.getProperty("bootstrap.servers"))
            && (FAKE_REGISTRY + "apis/ccompat/v6").equals(p.getProperty("schema.registry.url"))
            && "SSL".equals(p.getProperty("security.protocol"))
            && "all".equals(p.getProperty("acks"))
            && 0 == (int) p.get("retries")
            && StringSerializer.class.equals(p.get("key.serializer"))
            && FAKE_NAMING_STRATEGY.getStrategy().equals(p.get("value.subject.name.strategy"));

  private Predicate<ProducerRecord<String, IndexedRecord>> prodRecMatcher(
      final String topic, final String name) {
    return pr -> {
        var schema = pr.value().getSchema();
        return topic.equals(pr.topic())
                && name.equals(schema.getName()) // namespace from the testing file
                && "com.redhat.schema.pusher.avro.schemas".equals(schema.getNamespace());
      };
  }

  private Path getResourceAbsPath(final String resourceName) throws URISyntaxException {
    return Paths.get(getClass().getClassLoader().getResource(resourceName).toURI());
  }
}
