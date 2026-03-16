package io.vertx.micrometer.tests;

import io.netty.util.internal.PlatformDependent;
import io.vertx.core.http.*;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.test.tls.Cert;
import org.junit.Assume;

import java.util.List;
import java.util.Set;

public class VertxQuicHttpServerMetricsTest extends VertxHttpServerMetricsTestBase {

  private static final HttpServerConfig SERVER_CONFIG = new HttpServerConfig()
    .setVersions(Set.of(HttpVersion.HTTP_3));
  private static final HttpClientConfig CLIENT_CONFIG = new HttpClientConfig()
    .setVersions(List.of(HttpVersion.HTTP_3));

  public VertxQuicHttpServerMetricsTest() {
    super(SERVER_CONFIG, CLIENT_CONFIG);
  }

  @Override
  public void shouldDecrementActiveRequestsWhenRequestEndedAfterResponseEnded(TestContext ctx) {
    Assume.assumeFalse(PlatformDependent.isWindows());
    super.shouldDecrementActiveRequestsWhenRequestEndedAfterResponseEnded(ctx);
  }

  @Override
  public void serverName(TestContext ctx) {
    Assume.assumeFalse(PlatformDependent.isWindows());
    super.serverName(ctx);
  }
}
