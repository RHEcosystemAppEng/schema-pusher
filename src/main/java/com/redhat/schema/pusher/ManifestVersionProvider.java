package com.redhat.schema.pusher;

import java.io.IOException;
import java.util.jar.Manifest;
import picocli.CommandLine.IVersionProvider;

/**
 * Helper class implementing {@code picocli.CommandLine.IVersionProvider} providing versioning via
 * manifest entries.
 * Use {@value #APP_NAME_KEY} and {@value #APP_VERSION_KEY}.
 */
public final class ManifestVersionProvider implements IVersionProvider {
  private static final String APP_NAME_KEY = "PushCLI-Application-Name";
  private static final String APP_VERSION_KEY = "PushCLI-Application-Version";
  private static final String DEFAULT_VERSION = "Default Name - 0.0.0";

  @Override
  public String[] getVersion() throws Exception {
    var resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
          var url = resources.nextElement();
          try {
              var manifest = new Manifest(url.openStream());
              var attributes = manifest.getMainAttributes();
              if (attributes.containsKey(APP_NAME_KEY) && attributes.containsKey(APP_VERSION_KEY)) {
                return new String[] {
                  String.format("%s - %s", attributes.getValue(APP_NAME_KEY), attributes.getValue(APP_VERSION_KEY)) };
              }
          } catch (final IOException ioe) {
              return new String[] { DEFAULT_VERSION };
          }
      }
      return new String[] { DEFAULT_VERSION };
  }
}
