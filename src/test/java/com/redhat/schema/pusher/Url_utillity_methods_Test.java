package com.redhat.schema.pusher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Url_utillity_methods_Test {
  @ParameterizedTest
  @MethodSource
  void verify_the_cleanUrlEnd_func_removes_the_last_forward_slash(
      final String given, final String expected) {
    assertThat(UrlUtils.cleanUrlEnd.apply(given)).isEqualTo(expected);
  }

  static List<Arguments> verify_the_cleanUrlEnd_func_removes_the_last_forward_slash() {
    return List.of(
        arguments("http://no-fwd-slash-url.com", "http://no-fwd-slash-url.com"),
        arguments("http://with-fwd-slash-url/", "http://with-fwd-slash-url"));
  }

  @Test
  void verify_the_concatConfluentMap_func_adds_confluents_api_endpoint_map() {
    assertThat(UrlUtils.concatConfluentMap.apply("http://apiurl.exampl.com"))
        .isEqualTo("http://apiurl.exampl.com/apis/ccompat/v6");
  }

  @ParameterizedTest
  @MethodSource
  void verify_the_isSecured_utility_method_identifies_secure_urls(
      final String given, final boolean expected) {
    assertThat(UrlUtils.isSecured(given)).isEqualTo(expected);
  }

  static List<Arguments> verify_the_isSecured_utility_method_identifies_secure_urls() {
    return List.of(
        arguments("http://not-secured-url.example.com", false),
        arguments("https://secured-url.example.com", true));
  }
}
