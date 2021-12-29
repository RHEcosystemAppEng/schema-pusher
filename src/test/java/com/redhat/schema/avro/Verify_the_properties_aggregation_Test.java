package com.redhat.schema.avro;

import static org.assertj.core.api.BDDAssertions.then;

import com.redhat.schema.NamingStrategy;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

class Verify_the_properties_aggregation_Test {
  @CartesianTest
  void verify_properties_assignment_for_all_naming_strategies_and_urls_combinations(
    @Values(strings = {"http://fake-no-secured-bootstrap", "https://fake-secured-bootstrap"}) final String bootstrapUrl,
    @Values(strings = {"http://fakeregistry.example", "http://fakeregistry.example/"}) final String registryUrl,
    @Enum NamingStrategy strategy
  ) throws InterruptedException{
    // when instantiating a PropertiesAggregator instance
    var sut = new AvroProperties(bootstrapUrl, registryUrl, strategy);
    // then verify the properties values
    var props = sut.getProperties();
    then(props.getProperty("bootstrap.servers")).isEqualTo(bootstrapUrl);
    then(props.getProperty("schema.registry.url")).isEqualTo("http://fakeregistry.example/apis/ccompat/v6");
    then(props.getProperty("security.protocol")).isEqualTo(bootstrapUrl.startsWith("https://") ? "SSL" : "PLAINTEXT");
    then(props.getProperty("acks")).isEqualTo("all");
    then(props.get("retries")).isEqualTo(0);
    then(props.get("key.serializer")).isEqualTo(StringSerializer.class);
    then(props.get("value.subject.name.strategy")).isEqualTo(strategy.getStrategy());
  }
}
