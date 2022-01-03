package com.redhat.schema.pusher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Verify_manifest_version_provider_utility_Test {
  @Test
  void fetching_the_version_info_with_a_testing_manifest_should_return_the_expected_version() throws Exception {
    var sut = new ManifestVersionProvider();
    assertThat(sut.getVersion()).containsOnly("Fake Application Name 1.2.3-FAKE");
  }
}
