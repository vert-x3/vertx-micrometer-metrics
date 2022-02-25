package io.vertx.micrometer;

import io.micrometer.core.instrument.Tag;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.vertx.micrometer.RegistryInspector.Datapoint;
import static io.vertx.micrometer.RegistryInspector.listDatapoints;
import static io.vertx.micrometer.RegistryInspector.startsWith;
import static io.vertx.micrometer.RegistryInspector.waitForValue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxHttpServerMetricsConfigTest {
  private final String registryName = UUID.randomUUID().toString();
  private HttpServer httpServer;
  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportHttpServerMetricsWithCustomTags(TestContext ctx) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
      .setServerRequestTagsProvider(req -> {
        String user = req.headers().get("user");
        return Collections.singletonList(Tag.of("user", user));
      })
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setRegistryName(registryName)
      .setEnabled(true)))
      .exceptionHandler(ctx.exceptionHandler());

    prepareServer(ctx);
    HttpClient client = vertx.createHttpClient();
    sendRequest(ctx, client, "alice");
    sendRequest(ctx, client, "bob");

    waitForValue(vertx, ctx, registryName, "vertx.http.client.response.time[code=200,method=POST]$COUNT",
      value -> value.intValue() == 2);
    waitForValue(vertx, ctx, registryName, "vertx.http.client.active.requests[method=POST]$VALUE",
      value -> value.intValue() == 0);

    List<Datapoint> datapoints = listDatapoints(registryName, startsWith("vertx.http.server."));
    assertThat(datapoints).extracting(Datapoint::id).contains(
      "vertx.http.server.requests[code=200,method=POST,route=,user=alice]$COUNT",
      "vertx.http.server.active.requests[method=POST,user=alice]$VALUE",
      "vertx.http.server.response.time[code=200,method=POST,route=,user=alice]$COUNT",
      "vertx.http.server.requests[code=200,method=POST,route=,user=bob]$COUNT",
      "vertx.http.server.active.requests[method=POST,user=bob]$VALUE",
      "vertx.http.server.response.time[code=200,method=POST,route=,user=bob]$COUNT");
  }

  private void prepareServer(TestContext ctx) {
    // Setup server
    Async serverReady = ctx.async();
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Promise<Void> future) throws Exception {
        httpServer = vertx.createHttpServer();
        httpServer
          .requestHandler(req -> {
            vertx.setTimer(30L, handler ->
              req.response().setChunked(true).putHeader("Content-Type", "text/plain").end(""));
          })
          .listen(9195, "127.0.0.1", r -> {
            if (r.failed()) {
              ctx.fail(r.cause());
            } else {
              serverReady.complete();
            }
          });
      }
    });
    serverReady.awaitSuccess();
  }

  private void sendRequest(TestContext ctx, HttpClient client, String user) {
    Async async = ctx.async();
    client.request(HttpMethod.POST, 9195, "127.0.0.1", "/")
      .compose(req -> req
        .putHeader("user", user)
        .send("")
        .compose(response -> {
          if (response.statusCode() != 200) {
            return Future.failedFuture(response.statusMessage());
          } else {
            return response.body();
          }
        }))
      .onComplete(ctx.asyncAssertSuccess(v -> async.countDown()));
    async.await();
  }
}
