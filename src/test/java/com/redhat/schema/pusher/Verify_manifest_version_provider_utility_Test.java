package com.redhat.schema.pusher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Verify_manifest_version_provider_utility_Test {
  @Test
  void fetching_the_version_info_without_a_manifest_should_return_the_default_values() throws Exception {
    var sut = new ManifestVersionProvider();
    assertThat(sut.getVersion()).containsOnly("Default Name - 0.0.0");
  }
}
