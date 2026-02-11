package io.vertx.micrometer.tests;

import io.netty.util.internal.PlatformDependent;
import io.vertx.core.http.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public abstract class VertxHttpServerMetricsTestBase extends MicrometerMetricsTestBase {

  private final HttpServerConfig serverConfig;
  private final HttpClientConfig clientConfig;
  private HttpServer httpServer;

  public VertxHttpServerMetricsTestBase(HttpServerConfig serverConfig,  HttpClientConfig clientConfig) {
    this.serverConfig = serverConfig;
    this.clientConfig = clientConfig;
  }

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions()
      .addLabels(Label.HTTP_PATH);
  }

  @Test
  public void shouldDecrementActiveRequestsWhenRequestEndedAfterResponseEnded(TestContext ctx) {
    vertx = vertx(ctx);
    int numRequests = 10;
    Async doneLatch = ctx.async(numRequests * 2);
    httpServer = vertx.createHttpServer(serverConfig)
      .requestHandler(req -> {
        req.response().end(req.version().name());
        req.end().onComplete(ctx.asyncAssertSuccess(v -> doneLatch.countDown()));
      });
    Async listenLatch = ctx.async();
    httpServer
      .listen(9195, "127.0.0.1")
      .onComplete(ctx.asyncAssertSuccess(s -> listenLatch.complete()));
    listenLatch.awaitSuccess(20_000);
    HttpClient client = vertx.createHttpClient(clientConfig);
    List<HttpVersion> versions = clientConfig.getVersions();
    for (int i = 0;i < numRequests;i++) {
      RequestOptions request = new RequestOptions()
        .setProtocolVersion(versions.get(i % versions.size()))
        .setMethod(HttpMethod.POST)
        .setHost("127.0.0.1")
        .setPort(9195)
        .setURI("/resource?foo=bar");
      client.request(request)
        .onComplete(ctx.asyncAssertSuccess(req -> {
          req
            .response()
            .compose(HttpClientResponse::body)
            .onComplete(ctx.asyncAssertSuccess(b -> {
              HttpVersion version = HttpVersion.valueOf(b.toString());
              ctx.assertTrue(versions.contains(version));
              doneLatch.countDown();
              req.end();
            }));
          req.setChunked(true);
          req.write("chunk");
        }));
    }
    doneLatch.awaitSuccess(20_000);
    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.http.server.active.requests"));
    assertThat(datapoints).hasSize(1).contains(
      dp("vertx.http.server.active.requests[method=POST,path=/resource]$VALUE", 0.0));
  }
}
