package io.vertx.micrometer.impl;

/**
 * Copied over from Vert.x core HttpUtils to avoid exposing internal utilities.
 */
class HttpUtils {

  /**
   * Extract the path out of the uri.
   */
  static String parsePath(String uri) {
    if (uri.isEmpty()) {
      return "";
    }
    int i;
    if (uri.charAt(0) == '/') {
      i = 0;
    } else {
      i = uri.indexOf("://");
      if (i == -1) {
        i = 0;
      } else {
        i = uri.indexOf('/', i + 3);
        if (i == -1) {
          // contains no /
          return "/";
        }
      }
    }

    int queryStart = uri.indexOf('?', i);
    if (queryStart == -1) {
      queryStart = uri.length();
      if (i == 0) {
        return uri;
      }
    }
    return uri.substring(i, queryStart);
  }

  private HttpUtils() {
    // Utility
  }
}
