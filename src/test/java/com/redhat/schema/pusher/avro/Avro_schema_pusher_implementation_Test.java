// editorconfig-checker-disable-max-line-length
package com.redhat.schema.pusher.avro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.redhat.schema.pusher.NamingStrategy;
import com.redhat.schema.pusher.PushCli;
import com.redhat.schema.pusher.PushCli.KeystoreInfo;
import com.redhat.schema.pusher.PushCli.TruststoreInfo;
import com.redhat.schema.pusher.TopicAndSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

/** Test cases for the AVRO schema pusher impelementation. */
@ExtendWith(MockitoExtension.class)
class Avro_schema_pusher_implementation_Test {
  private static final String FAKE_SECURED_BOOTSTRAP = "https://fake-kafka-bootstrap:443";
  private static final String FAKE_NOT_SECURED_BOOTSTRAP = "http://fake-kafka-bootstrap/";
  private static final String FAKE_NOT_SECURED_BOOTSTRAP_CLEAN = "http://fake-kafka-bootstrap";
  private static final String FAKE_REGISTRY = "http://fake-redhat-service-registry/";
  private static final NamingStrategy FAKE_NAMING_STRATEGY = NamingStrategy.TOPIC_RECORD;
  private static final String FAKE_TOPIC1 = "faketopic";
  private static final String FAKE_TOPIC2 = "anotherfaketopic";
  private static final String FAKE_TRUSTSTORE_FILE = "/path/to/truststore.p12";
  private static final String FAKE_TRUSTSTORE_PASSWORD = "hideme123#@!";
  private static final String FAKE_KEYSTORE_FILE = "/path/to/keystore.p12";
  private static final String FAKE_KEYSTORE_PASSWORD = "hidemetoo123#@!";

  @Captor private ArgumentCaptor<ProducerRecord<String, IndexedRecord>> prodRecCaptore;
  @Mock private Callback mockCallback;
  @Mock private KafkaProducer<String, IndexedRecord> mockProducer;
  @Mock private ApplicationContext mockContext;
  @Mock private PushCli mockCli;
  private AvroSchemaPusher sut;

  @Test
  @SuppressWarnings("unchecked")
  void a_failed_push_with_the_producer_should_not_throw_an_exception() throws URISyntaxException {
    // stub the cli
    when(mockCli.getKafkaBootstrap()).thenReturn(FAKE_SECURED_BOOTSTRAP);
    when(mockCli.getServiceRegistry()).thenReturn(FAKE_REGISTRY);
    when(mockCli.getNamingStrategy()).thenReturn(FAKE_NAMING_STRATEGY);
    // instantiate the sut with the fake info
    sut = new AvroSchemaPusher(mockCli);
    // stub the private di context
    setField(sut, "context", mockContext);
    // stub the mocked context to return a mocked callback for the specific arguments
    given(mockContext.getBean(eq(Callback.class), any(Logger.class), eq("test_schema1.avsc"), eq(FAKE_TOPIC1))).willReturn(mockCallback);
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // short-circuit the producer to throw an exception
    given(mockProducer.send(any(ProducerRecord.class), eq(mockCallback))).willThrow(new UncheckedIOException(new IOException("fake exception")));
    // given the di context will return the mocked producer as bean per the properties match
    given(mockContext.getBean(eq(KafkaProducer.class), argThat(securedPropertiesMatcher))).willReturn(mockProducer);
    // given the following test schema
    var testSchema = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema1.avsc");
    // when invoking the push method with it no exceptions should be thrown
    assertThatNoException().isThrownBy(() -> sut.push(List.of(new TopicAndSchema(FAKE_TOPIC1, testSchema))));
    // and the mock producer was invoked
    then(mockProducer).should().send(any(ProducerRecord.class), eq(mockCallback));
  }

  @Test
  void pushing_one_file_and_one_topic_should_result_in_one_producer_records_sent(
      @Mock final TruststoreInfo mockTruststoreInfo,
      @Mock final KeystoreInfo mockKeystoreInfo) throws URISyntaxException {
    // stub the truststore info
    given(mockTruststoreInfo.getTruststoreFile()).willReturn(FAKE_TRUSTSTORE_FILE);
    given(mockTruststoreInfo.getTruststorePassword()).willReturn(FAKE_TRUSTSTORE_PASSWORD);
    // stub the keystore info
    given(mockKeystoreInfo.getKeystoreFile()).willReturn(FAKE_KEYSTORE_FILE);
    given(mockKeystoreInfo.getKeystorePassword()).willReturn(FAKE_KEYSTORE_PASSWORD);
    // stub the cli
    given(mockCli.getKafkaBootstrap()).willReturn(FAKE_SECURED_BOOTSTRAP);
    given(mockCli.getServiceRegistry()).willReturn(FAKE_REGISTRY);
    given(mockCli.getNamingStrategy()).willReturn(FAKE_NAMING_STRATEGY);
    given(mockCli.getTruststoreInfo()).willReturn(mockTruststoreInfo);
    given(mockCli.getKeystoreInfo()).willReturn(mockKeystoreInfo);
    // instantiate the sut with the fake info
    sut = new AvroSchemaPusher(mockCli);
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // stub the private di context
    setField(sut, "context", mockContext);
    // stub the mocked context to return a mocked callback for the specific arguments
    given(mockContext.getBean(eq(Callback.class), any(Logger.class), eq("test_schema1.avsc"), eq(FAKE_TOPIC1))).willReturn(mockCallback);
    // given the di context will return the mocked producer as bean per the properties match
    given(mockContext.getBean(eq(KafkaProducer.class), argThat(selfSingedPropertiesMatcher))).willReturn(mockProducer);
    // given the following two schema test files
    var testSchema1 = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema1.avsc");
    // when invoking the push method with two topics and two schema files
    sut.push(List.of(new TopicAndSchema(FAKE_TOPIC1, testSchema1)));
    // one invocation of the send method with expected arguments including the mocked callback
    then(mockProducer).should().send(argThat(prodRecMatcher(FAKE_TOPIC1, "TestingSchema1Name")::test), eq(mockCallback));
    // and one invocation for flushing the producer cache
    then(mockProducer).should().flush();
  }

  @Test
  void pushing_two_files_and_two_topics_should_result_in_two_producer_records_sent() throws URISyntaxException {
    // stub the cli
    when(mockCli.getKafkaBootstrap()).thenReturn(FAKE_NOT_SECURED_BOOTSTRAP);
    when(mockCli.getServiceRegistry()).thenReturn(FAKE_REGISTRY);
    when(mockCli.getNamingStrategy()).thenReturn(FAKE_NAMING_STRATEGY);
    // instantiate the sut with the fake info
    sut = new AvroSchemaPusher(mockCli);
    // turn off the sut's logger to avoid polluting the build log
    ((Logger) getField(sut, "LOGGER")).setLevel(Level.OFF);
    // stub the private di context
    setField(sut, "context", mockContext);
    // stub the mocked context to return a mocked callback for the specific arguments
    given(mockContext.getBean(eq(Callback.class), any(Logger.class), anyString(), anyString())).willReturn(mockCallback);
    // given the di context will return the mocked producer as bean per the properties match
    given(mockContext.getBean(eq(KafkaProducer.class), argThat(notSecuredPropertiesMatcher))).willReturn(mockProducer);
    // given the following two schema test files
    var testSchema1 = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema1.avsc");
    var testSchema2 = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema2.avsc");
    // when invoking the push method with two topics and two schema files
    sut.push(List.of(
      new TopicAndSchema(FAKE_TOPIC1, testSchema1),
      new TopicAndSchema(FAKE_TOPIC2, testSchema2)));
    // then producer should be invoked in order
    var mockProdOrder = inOrder(mockProducer);
    // four invocations of the send method, one per schema+topic
    then(mockProducer).should(mockProdOrder, times(2)).send(prodRecCaptore.capture(), eq(mockCallback));
    // and one invocation for flushing the producer cache
    then(mockProducer).should(mockProdOrder).flush();
    // verify each schema was sent to each topic (schema names are from in the schema files)
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC1, "TestingSchema1Name"))
        .hasSize(1);
    assertThat(prodRecCaptore.getAllValues())
        .filteredOn(prodRecMatcher(FAKE_TOPIC2, "TestingSchema2Name"))
        .hasSize(1);
  }

  private ArgumentMatcher<Properties> securedPropertiesMatcher =
      p -> FAKE_SECURED_BOOTSTRAP.equals(p.getProperty("bootstrap.servers"))
            && (FAKE_REGISTRY + "apis/ccompat/v6").equals(p.getProperty("schema.registry.url"))
            && "SSL".equals(p.getProperty("security.protocol"))
            && "all".equals(p.getProperty("acks"))
            && 0 == (int) p.get("retries")
            && StringSerializer.class.equals(p.get("key.serializer"))
            && FAKE_NAMING_STRATEGY.getStrategy().equals(p.get("value.subject.name.strategy"));

  private ArgumentMatcher<Properties> notSecuredPropertiesMatcher =
      p -> FAKE_NOT_SECURED_BOOTSTRAP_CLEAN.equals(p.getProperty("bootstrap.servers"))
            && (FAKE_REGISTRY + "apis/ccompat/v6").equals(p.getProperty("schema.registry.url"))
            && "all".equals(p.getProperty("acks"))
            && 0 == (int) p.get("retries")
            && StringSerializer.class.equals(p.get("key.serializer"))
            && FAKE_NAMING_STRATEGY.getStrategy().equals(p.get("value.subject.name.strategy"));

  private ArgumentMatcher<Properties> selfSingedPropertiesMatcher =
      p -> FAKE_SECURED_BOOTSTRAP.equals(p.getProperty("bootstrap.servers"))
            && (FAKE_REGISTRY + "apis/ccompat/v6").equals(p.getProperty("schema.registry.url"))
            && "SSL".equals(p.getProperty("security.protocol"))
            && "all".equals(p.getProperty("acks"))
            && 0 == (int) p.get("retries")
            && StringSerializer.class.equals(p.get("key.serializer"))
            && FAKE_NAMING_STRATEGY.getStrategy().equals(p.get("value.subject.name.strategy"))
            && FAKE_TRUSTSTORE_FILE.equals(p.getProperty("ssl.truststore.location"))
            && FAKE_TRUSTSTORE_PASSWORD.equals(p.getProperty("ssl.truststore.password"))
            && "PKCS12".equals(p.getProperty("ssl.truststore.type"))
            && FAKE_KEYSTORE_FILE.equals(p.getProperty("ssl.keystore.location"))
            && FAKE_KEYSTORE_PASSWORD.equals(p.getProperty("ssl.keystore.password"))
            && "PKCS12".equals(p.getProperty("ssl.keystore.type"));

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
