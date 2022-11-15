package com.redhat.schema.pusher;

import java.util.function.UnaryOperator;

/** Utility class for working with URL strings. */
public final class UrlUtils {
  private static final String CONFLUENT_COMPAT_MAP_FMT = "%s/apis/ccompat/v6";

  private UrlUtils() {
    // a utility class needs no instantiation
  }

  /** Function for concatenating {@value #CONFLUENT_COMPAT_MAP_FMT} to a string url. */
  public static final UnaryOperator<String> concatConfluentMap =
      url -> String.format(CONFLUENT_COMPAT_MAP_FMT, url);

  /** Function for removing the final forward slash (/) char if exists from a string url. */
  public static final UnaryOperator<String> cleanUrlEnd =
      url -> url.charAt(url.length() - 1) == '/' ? url.substring(0, url.length() - 1) : url;

  /**
   * Check if a url is secured.
   *
   * @param url the url string.
   * @return true if the url start with 'https://'.
   */
  public static boolean isSecured(final String url) {
    return url.startsWith("https://");
  }
}
