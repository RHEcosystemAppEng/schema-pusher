package com.redhat.schema.pusher.avro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.redhat.schema.pusher.avro.AvroSchemaPusher;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Avro_schema_pusher_implementation_Test {
  @Mock private KafkaProducer<String, IndexedRecord> mockProducer;
  @Spy private Schema.Parser spyParser;

  @InjectMocks private AvroSchemaPusher sut;

  @Test
  void invoke_the_overridden_push_method_with_a_testing_schema_and_a_topic(
      @Mock final Future<RecordMetadata> mockFuture) throws IOException, URISyntaxException {
    // given the following schema test file, fake topic, and argument matcher
    var testSchemaFile = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema.avsc");
    var testTopic = "faketopic";
    var argMatcher = getArgMatcher(testTopic);
    // given the mocked producer will return the mocked future for args that matches the arg matcher
    given(mockProducer.send(argThat(argMatcher))).willReturn(mockFuture);
    // when invoking the push method on the pusher sut
    //var retFuture = sut.push(testSchemaFile, testTopic);
    // verify the return future is the mocked future
    //assertThat(retFuture).isEqualTo(mockFuture);
  }

  @Test
  void invoke_the_overloaded_push_method_with_a_parser_a_testing_schema_and_a_topic(
      @Mock final Future<RecordMetadata> mockFuture) throws IOException, URISyntaxException {
    // given the following schema test file, fake topic, and argument matcher
    var testSchemaFile = getResourceAbsPath("com/redhat/schema/pusher/avro/schemas/test_schema.avsc");
    var testTopic = "anotherfaketopic";
    var argMatcher = getArgMatcher(testTopic);
    // given the mocked producer will return the mocked future for args that matches the arg matcher
    given(mockProducer.send(argThat(argMatcher))).willReturn(mockFuture);
    // when invoking the push method on the pusher sut with the spy parser
    //var retFuture = sut.push(spyParser, testSchemaFile, testTopic);
    // then the spy parser should have been invoked once
    then(spyParser).should().parse(any(InputStream.class));
    // and the return future is the mocked future
    //assertThat(retFuture).isEqualTo(mockFuture);
  }

  private Path getResourceAbsPath(final String resourceName) throws URISyntaxException {
    return Paths.get(getClass().getClassLoader().getResource(resourceName).toURI());
  }

  private ArgumentMatcher<ProducerRecord<String, IndexedRecord>> getArgMatcher(final String topic) {
    return a -> {
      var schema = a.value().getSchema();
      return a.topic().equals(topic)
          && schema.getName().equals("TestingSchema") // name from the testing file
          && schema.getNamespace().equals("com.redhat.schema.pusher.avro.schemas"); // namespace from the testing file
    };
  }
}
