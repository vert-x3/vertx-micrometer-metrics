package io.vertx.monitoring.service;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.VertxMonitoringOptions;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class MetricsServiceImplTest {
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";

  private final String registryName = UUID.randomUUID().toString();
  private HttpServer httpServer;
  private Vertx vertx;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new VertxMonitoringOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setRegistryName(registryName)
      .setEnabled(true)))
      .exceptionHandler(ctx.exceptionHandler());

    // Setup server
    Async serverReady = ctx.async();
    httpServer = vertx.createHttpServer();
    httpServer
      .requestHandler(req -> {
        // Timer as artificial processing time
        vertx.setTimer(30L, handler ->
          req.response().setChunked(true).putHeader("Content-Type", "text/plain").write(SERVER_RESPONSE).end());
      })
      .listen(9195, "127.0.0.1", r -> {
        if (r.failed()) {
          ctx.fail(r.cause());
        } else {
          serverReady.complete();
        }
      });
    serverReady.awaitSuccess();
  }

  @After
  public void teardown() {
    if (httpServer != null) {
      httpServer.close();
    }
  }

  @Test
  public void shouldGetCompleteSnapshot(TestContext ctx) throws InterruptedException {
    HttpClient httpClient = vertx.createHttpClient();
    runClientRequests(ctx, httpClient, 10, "/r1");
    runClientRequests(ctx, httpClient, 5, "/r2");
    httpClient.close();

    JsonObject snapshot = MetricsService.create(vertx).getMetricsSnapshot();
    assertThat(snapshot).extracting(Map.Entry::getKey).containsExactly(
      "vertx.http.client.bytesReceived",
      "vertx.http.client.bytesSent",
      "vertx.http.client.connections",
      "vertx.http.client.requestCount",
      "vertx.http.client.requests",
      "vertx.http.client.responseCount",
      "vertx.http.client.responseTime",
      "vertx.http.server.bytesReceived",
      "vertx.http.server.bytesSent",
      "vertx.http.server.connections",
      "vertx.http.server.requestCount",
      "vertx.http.server.requests",
      "vertx.http.server.responseTime");

    assertThat(snapshot).flatExtracting(e -> (List<JsonObject>)((JsonArray)(e.getValue())).getList())
      .filteredOn(obj -> obj.getString("type").equals("counter"))
      .hasSize(6)
      .flatExtracting(JsonObject::fieldNames)
      .contains("count");

    assertThat(snapshot).flatExtracting(e -> (List<JsonObject>)((JsonArray)(e.getValue())).getList())
      .filteredOn(obj -> obj.getString("type").equals("summary"))
      .hasSize(4)
      .flatExtracting(JsonObject::fieldNames)
      .contains("mean", "max", "total");

    assertThat(snapshot).flatExtracting(e -> (List<JsonObject>)((JsonArray)(e.getValue())).getList())
      .filteredOn(obj -> obj.getString("type").equals("timer"))
      .hasSize(4)
      .flatExtracting(JsonObject::fieldNames)
      .contains("totalTimeMs", "meanMs", "maxMs");
  }

  @Test
  public void shouldGetHttpServerSnapshot(TestContext ctx) throws InterruptedException {
    HttpClient httpClient = vertx.createHttpClient();
    runClientRequests(ctx, httpClient, 5, "/r2");
    httpClient.close();

    JsonObject snapshot = MetricsService.create(httpServer).getMetricsSnapshot();
    assertThat(snapshot).extracting(Map.Entry::getKey).containsExactly(
      "vertx.http.server.bytesReceived",
      "vertx.http.server.bytesSent",
      "vertx.http.server.connections",
      "vertx.http.server.requestCount",
      "vertx.http.server.requests",
      "vertx.http.server.responseTime");
  }

  private void runClientRequests(TestContext ctx, HttpClient httpClient, int count, String path) {
    Async async = ctx.async(count);
    for (int i = 0; i < count; i++) {
      httpClient.post(9195, "127.0.0.1", path, response -> {
        async.countDown();
        if (response.statusCode() != 200) {
          ctx.fail(response.statusMessage());
        }
      }).exceptionHandler(t -> {
        async.countDown();
        ctx.fail(t);
      }).putHeader("Content-Length", String.valueOf(CLIENT_REQUEST.getBytes().length))
        .write(CLIENT_REQUEST)
        .end();
    }
    async.await();
  }
}
