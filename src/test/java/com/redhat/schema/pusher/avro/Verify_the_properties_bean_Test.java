package com.redhat.schema.pusher.avro;

class Verify_the_properties_bean_Test {
  // @CartesianTest
  // void verify_properties_assignment_for_all_naming_strategies_and_urls_combinations(
  //   @Values(strings = {"http://fake-no-secured-bootstrap", "https://fake-secured-bootstrap"})
  // final String bootstrapUrl,
  //   @Values(strings = {"http://fakeregistry.example", "http://fakeregistry.example/"}) final
  // String registryUrl,
  //   @Enum NamingStrategy strategy
  // ) throws InterruptedException{
  //   // when instantiating a PropertiesAggregator instance
  //   var sut = new AvroBeansConfig().producerProperties(bootstrapUrl, registryUrl,
  // strategy.toString());
  //   // then verify the properties values
  //   then(sut.getProperty("bootstrap.servers")).isEqualTo(bootstrapUrl);
  //
  // then(sut.getProperty("schema.registry.url")).isEqualTo("http://fakeregistry.example/apis/ccompat/v6");
  //   then(sut.getProperty("security.protocol")).isEqualTo(bootstrapUrl.startsWith("https://") ?
  // "SSL" : "PLAINTEXT");
  //   then(sut.getProperty("acks")).isEqualTo("all");
  //   then(sut.get("retries")).isEqualTo(0);
  //   then(sut.get("key.serializer")).isEqualTo(StringSerializer.class);
  //   then(sut.get("value.subject.name.strategy")).isEqualTo(strategy.getStrategy());
  // }
}
