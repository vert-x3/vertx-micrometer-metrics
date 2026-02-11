package io.vertx.micrometer.tests;

import io.vertx.core.http.*;

import java.util.List;
import java.util.Set;

public class VertxTcpHttpServerMetricsTest extends VertxHttpServerMetricsTestBase {

  private static final HttpServerConfig SERVER_CONFIG = new HttpServerConfig()
    .setVersions(Set.of(HttpVersion.HTTP_1_1));
  private static final HttpClientConfig CLIENT_CONFIG = new HttpClientConfig()
    .setVersions(List.of(HttpVersion.HTTP_1_1));

  public VertxTcpHttpServerMetricsTest() {
    super(SERVER_CONFIG, CLIENT_CONFIG);
  }
}
