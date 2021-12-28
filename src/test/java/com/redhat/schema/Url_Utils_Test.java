package com.redhat.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class Url_Utils_Test {
  @ParameterizedTest
  @MethodSource
  void test_cleanUrlEnd_func_with_given_and_expected(final String given, final String expected) {
    assertThat(UrlUtils.cleanUrlEnd.apply(given)).isEqualTo(expected);
  }

  static List<Arguments> test_cleanUrlEnd_func_with_given_and_expected() {
    return List.of(
      arguments("http://no-fwd-slash-url.com", "http://no-fwd-slash-url.com"),
      arguments("http://with-fwd-slash-url/", "http://with-fwd-slash-url")
    );
  }

  @Test
  void test_concatConfluentMap_func() {
    assertThat(UrlUtils.concatConfluentMap.apply("http://apiurl.exampl.com"))
      .isEqualTo("http://apiurl.exampl.com/apis/ccompat/v6");
  }

  @ParameterizedTest
  @MethodSource
  void test_isSecured_utility_method_with_given_and_expected(final String given, final boolean expected) {
    assertThat(UrlUtils.isSecured(given)).isEqualTo(expected);
  }

  static List<Arguments> test_isSecured_utility_method_with_given_and_expected() {
    return List.of(
      arguments("http://not-secured-url.example.com", false),
      arguments("https://secured-url.example.com", true)
    );
  }
}
