package io.vertx.micrometer.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class MetricsServiceImplTest extends MicrometerMetricsTestBase {
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";

  private HttpServer httpServer;

  private void setUpWithNames(TestContext ctx, MetricsNaming names) {
    metricsOptions
      .setLabels(EnumSet.complementOf(EnumSet.of(Label.LOCAL, Label.REMOTE)))
      .setMetricsNaming(names);

    vertx = vertx(ctx);

    // Setup server
    Async serverReady = ctx.async();
    httpServer = vertx.createHttpServer();
    httpServer
      .requestHandler(req -> {
        // Timer as artificial processing time
        vertx.setTimer(30L, handler ->
          req.response().setChunked(true).putHeader("Content-Type", "text/plain").end(SERVER_RESPONSE));
      })
      .listen(9195, "127.0.0.1").onComplete(r -> {
        if (r.failed()) {
          ctx.fail(r.cause());
        } else {
          serverReady.complete();
        }
      });
    serverReady.awaitSuccess();
  }

  @Test
  public void shouldGetCompleteSnapshot(TestContext ctx) {
    setUpWithNames(ctx, MetricsNaming.v4Names());

    HttpClient httpClient = vertx.createHttpClient();
    runClientRequests(ctx, httpClient, 10, "/r1");
    runClientRequests(ctx, httpClient, 5, "/r2");
    httpClient.close();

    JsonObject snapshot = MetricsService.create(vertx).getMetricsSnapshot();
    assertThat(snapshot).extracting(Map.Entry::getKey).containsExactly(
      "vertx.http.client.active.connections",
      "vertx.http.client.active.requests",
      "vertx.http.client.bytes.read",
      "vertx.http.client.bytes.written",
      "vertx.http.client.queue.pending",
      "vertx.http.client.queue.time",
      "vertx.http.client.request.bytes",
      "vertx.http.client.requests",
      "vertx.http.client.response.bytes",
      "vertx.http.client.response.time",
      "vertx.http.client.responses",
      "vertx.http.server.active.connections",
      "vertx.http.server.active.requests",
      "vertx.http.server.bytes.read",
      "vertx.http.server.bytes.written",
      "vertx.http.server.request.bytes",
      "vertx.http.server.requests",
      "vertx.http.server.response.bytes",
      "vertx.http.server.response.time"
    );

    assertThat(snapshot).flatExtracting(e -> (List<JsonObject>) ((JsonArray) (e.getValue())).getList())
      .filteredOn(obj -> obj.getString("type").equals("counter"))
      .hasSize(10)
      .flatExtracting(JsonObject::fieldNames)
      .contains("count");

    assertThat(snapshot).flatExtracting(e -> (List<JsonObject>) ((JsonArray) (e.getValue())).getList())
      .filteredOn(obj -> obj.getString("type").equals("timer"))
      .hasSize(5)
      .flatExtracting(JsonObject::fieldNames)
      .contains("totalTimeMs", "meanMs", "maxMs");
  }

  @Test
  public void shouldGetSnapshotWithV3Names(TestContext ctx) {
    setUpWithNames(ctx, MetricsNaming.v3Names());

    HttpClient httpClient = vertx.createHttpClient();
    runClientRequests(ctx, httpClient, 10, "/r1");
    runClientRequests(ctx, httpClient, 5, "/r2");
    httpClient.close();

    JsonObject snapshot = MetricsService.create(vertx).getMetricsSnapshot();
    assertThat(snapshot).extracting(Map.Entry::getKey).filteredOn(k -> k.startsWith("vertx.http")).containsExactly(
      "vertx.http.client.bytesReceived",
      "vertx.http.client.bytesSent",
      "vertx.http.client.connections",
      "vertx.http.client.queue.delay",
      "vertx.http.client.queue.size",
      "vertx.http.client.request.bytes",
      "vertx.http.client.requestCount",
      "vertx.http.client.requests",
      "vertx.http.client.response.bytes",
      "vertx.http.client.responseCount",
      "vertx.http.client.responseTime",
      "vertx.http.server.bytesReceived",
      "vertx.http.server.bytesSent",
      "vertx.http.server.connections",
      "vertx.http.server.request.bytes",
      "vertx.http.server.requestCount",
      "vertx.http.server.requests",
      "vertx.http.server.response.bytes",
      "vertx.http.server.responseTime"
    );
  }

  @Test
  public void shouldGetHttpServerSnapshot(TestContext ctx) {
    setUpWithNames(ctx, MetricsNaming.v4Names());

    HttpClient httpClient = vertx.createHttpClient();
    runClientRequests(ctx, httpClient, 5, "/r2");
    httpClient.close();

    JsonObject snapshot = MetricsService.create(httpServer).getMetricsSnapshot();
    assertThat(snapshot).extracting(Map.Entry::getKey).containsExactly(
      "vertx.http.server.active.connections",
      "vertx.http.server.active.requests",
      "vertx.http.server.bytes.read",
      "vertx.http.server.bytes.written",
      "vertx.http.server.request.bytes",
      "vertx.http.server.requests",
      "vertx.http.server.response.bytes",
      "vertx.http.server.response.time"
      );
  }

  private void runClientRequests(TestContext ctx, HttpClient httpClient, int count, String path) {
    Async async = ctx.async(count);
    for (int i = 0; i < count; i++) {
      httpClient.request(HttpMethod.POST, 9195, "127.0.0.1", path)
        .compose(req -> req.send(Buffer.buffer(CLIENT_REQUEST))
          .compose(resp -> {
            if (resp.statusCode() != 200) {
              return Future.failedFuture(resp.statusMessage());
            } else {
              return resp.body();
            }
          })).onComplete(ctx.asyncAssertSuccess(v -> async.countDown()));
    }
    async.await();
  }

  @Test
  public void shouldGetJvmMetricsInSnapshot(TestContext ctx) {
    metricsOptions = new MicrometerMetricsOptions()
      .setJvmMetricsEnabled(true)
      .setMicrometerRegistry(new SimpleMeterRegistry())
      .setRegistryName(registryName)
      .setEnabled(true);

    vertx = vertx(ctx);

    JsonObject snapshot = MetricsService.create(vertx).getMetricsSnapshot("jvm");

    assertFalse(snapshot.isEmpty());
  }
}
