package com.redhat.schema.pusher.avro;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Test cases for the custom record callback. */
@ExtendWith(MockitoExtension.class)
class Avro_record_callback_impelemntation_Test {
  private static final String FAKE_TOPIC = "faketopic";
  private static final String FAKE_FILENAME = "fake_schmea_file.json";

  @Mock private Logger mockLogger;
  @Mock private RecordMetadata mockRecordMetadata;
  private AvroRecordCallbak sut;

  @BeforeEach
  void initialize() {
    sut = new AvroRecordCallbak(mockLogger, FAKE_FILENAME, FAKE_TOPIC);
  }

  @SuppressWarnings("unchecked")
  @Test
  void invoking_on_completion_with_a_null_exception_should_log_an_info_message() {
    // when invoking the onCompletion overided method with the sut
    sut.onCompletion(mockRecordMetadata, null);
    then(mockLogger).should().info(any(Supplier.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void invoking_on_completion_with_a_non_null_exception_should_log_a_severe_message() {
    // given the following exception
    var expectedException = new Exception("fake execption");
    // when invoking the onCompletion overided method with the sut
    sut.onCompletion(mockRecordMetadata, expectedException);
    then(mockLogger).should().log(eq(Level.SEVERE), eq(expectedException), any(Supplier.class));
  }
}
