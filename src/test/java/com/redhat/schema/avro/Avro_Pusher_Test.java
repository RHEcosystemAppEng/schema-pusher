package com.redhat.schema.avro;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.IndexedRecord;
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
class Avro_Pusher_Test {
  @Mock private AvroProducer mockProducer;
  @Spy private Parser parser = new Parser();

  @InjectMocks private AvroPusher sut;

  @Test
  void test_push_method_with_testing_schema_and_topic(
      @Mock final Future<RecordMetadata> mockFuture) throws IOException, URISyntaxException {
    // given the following schema test file, fake topic, and argument matcher
    var testSchemaFile = getResourceAbsPath("com/redhat/schema/avro/schemas/test_schema.avsc");
    var testTopic = "faketopic";
    var argMatcher = getArgMatcher(testTopic);
    // given the mocked producer will return the mocked future for args that matches the arg matcher
    given(mockProducer.send(argThat(argMatcher))).willReturn(mockFuture);
    // when invoking the push method on the pusher sut
    var retFuture = sut.push(testSchemaFile, testTopic);
    // then the return future is the mocked future
    then(retFuture).isEqualTo(mockFuture);
  }

  private Path getResourceAbsPath(final String resourceName) throws URISyntaxException {
    return Paths.get(getClass().getClassLoader().getResource(resourceName).toURI());
  }

  private ArgumentMatcher<ProducerRecord<String, IndexedRecord>> getArgMatcher(final String topic) {
    return a -> {
      var schema = a.value().getSchema();
      return a.topic().equals(topic)
          && schema.getName().equals("TestingSchema") // name from the testing file
          && schema.getNamespace().equals("com.redhat.schema.avro.schemas"); // namespace from the testing file
    };
  }
}
