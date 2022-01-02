package com.redhat.schema.pusher;

import static java.util.Objects.nonNull;

import java.util.jar.Manifest;
import picocli.CommandLine.IVersionProvider;

/**
 * Helper class implementing {@code picocli.CommandLine.IVersionProvider} providing versioning via
 * manifest entries.
 * Use {@value #APP_NAME_KEY} and {@value #APP_VERSION_KEY} in your manifest.
 */
public final class ManifestVersionProvider implements IVersionProvider {
  private static final String APP_NAME_KEY = "PushCLI-Application-Name";
  private static final String APP_VERSION_KEY = "PushCLI-Application-Version";
  private static final String DEFAULT_VERSION = "Default Name 0.0.0";

  @Override
  public String[] getVersion() throws Exception {
    var manifests = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
    while (manifests.hasMoreElements()) {
      var url = manifests.nextElement();
      if (!url.toString().contains("schema-pusher")) {
        continue;
      }
      var manifest = new Manifest(url.openStream());
      var attributes = manifest.getMainAttributes();
      var appName = attributes.getValue(APP_NAME_KEY);
      var appVersion = attributes.getValue(APP_VERSION_KEY);
      if (nonNull(appName) && nonNull(appVersion)) {
        return new String[] { String.format("%s %s", appName, appVersion) };
      }
    }
    return new String[] { DEFAULT_VERSION };
  }
}
