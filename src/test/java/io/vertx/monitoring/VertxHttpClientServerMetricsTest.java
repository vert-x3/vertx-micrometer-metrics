package io.vertx.monitoring;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxHttpClientServerMetricsTest {

  private static final int HTTP_SENT_COUNT = 68;
  private static final int SENT_COUNT = HTTP_SENT_COUNT +1 ;
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";
  private static final long REQ_DELAY = 30L;

  private final int concurrentClients = ForkJoinPool.commonPool().getParallelism();
  private final String registryName = UUID.randomUUID().toString();
  private final List<HttpClient> createdClients = new CopyOnWriteArrayList<>();
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
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Future<Void> future) throws Exception {
        httpServer = vertx.createHttpServer();
        httpServer
          .websocketHandler(ws ->
            ws.handler(event -> {
              vertx.setTimer(REQ_DELAY, timer -> ws.writeTextMessage(SERVER_RESPONSE).end());
            }))
          .requestHandler(req -> {
            // Timer as artificial processing time
            vertx.setTimer(REQ_DELAY, handler ->
              req.response().setChunked(true).putHeader("Content-Type", "text/plain").write(SERVER_RESPONSE).end());
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

  @After
  public void teardown() {
    createdClients.forEach(HttpClient::close);
    if (httpServer != null) {
      httpServer.close();
    }
  }

  @Test
  public void shouldReportHttpClientMetrics(TestContext ctx) throws InterruptedException {
    runClientRequests(ctx, false);
    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.http.client.", registryName);
    assertThat(datapoints).hasSize(8).contains(
        dp("vertx.http.client.bytesReceived[local=?,remote=127.0.0.1:9195]$Count", concurrentClients * HTTP_SENT_COUNT),
        dp("vertx.http.client.bytesReceived[local=?,remote=127.0.0.1:9195]$Total", concurrentClients * HTTP_SENT_COUNT * SERVER_RESPONSE.getBytes().length),
        dp("vertx.http.client.bytesSent[local=?,remote=127.0.0.1:9195]$Count", concurrentClients * HTTP_SENT_COUNT),
        dp("vertx.http.client.bytesSent[local=?,remote=127.0.0.1:9195]$Total", concurrentClients * HTTP_SENT_COUNT * CLIENT_REQUEST.getBytes().length),
        dp("vertx.http.client.requestCount[local=?,method=POST,path=/resource,remote=127.0.0.1:9195]$Count", concurrentClients * HTTP_SENT_COUNT));

    List<RegistryInspector.Datapoint> timersDp = RegistryInspector.listTimers("vertx.http.client.", registryName);
    assertThat(timersDp).extracting(RegistryInspector.Datapoint::id).containsOnly(
      "vertx.http.client.responseTime[local=?,path=/resource,remote=127.0.0.1:9195]$TotalTime",
      "vertx.http.client.responseTime[local=?,path=/resource,remote=127.0.0.1:9195]$Count",
      "vertx.http.client.responseTime[local=?,path=/resource,remote=127.0.0.1:9195]$Max");
  }

  @Test
  public void shouldReportHttpServerMetrics(TestContext ctx) throws InterruptedException {
    runClientRequests(ctx, true);
    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.http.server.", registryName);
    assertThat(datapoints).extracting(RegistryInspector.Datapoint::id).containsOnly(
      "vertx.http.server.requestCount[code=200,local=127.0.0.1:9195,method=POST,path=/resource,remote=_]$Count",
      "vertx.http.server.requests[local=127.0.0.1:9195,path=/resource,remote=_]$Value",
      "vertx.http.server.connections[local=127.0.0.1:9195,remote=_]$Value",
      "vertx.http.server.wsConnections[local=127.0.0.1:9195,remote=_]$Value",
      "vertx.http.server.bytesReceived[local=127.0.0.1:9195,remote=_]$Count",
      "vertx.http.server.bytesReceived[local=127.0.0.1:9195,remote=_]$Total",
      "vertx.http.server.bytesSent[local=127.0.0.1:9195,remote=_]$Count",
      "vertx.http.server.bytesSent[local=127.0.0.1:9195,remote=_]$Total");
    assertThat(datapoints).contains(
      dp("vertx.http.server.bytesReceived[local=127.0.0.1:9195,remote=_]$Count", concurrentClients * SENT_COUNT),
      dp("vertx.http.server.bytesReceived[local=127.0.0.1:9195,remote=_]$Total", concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length),
      dp("vertx.http.server.bytesSent[local=127.0.0.1:9195,remote=_]$Count", concurrentClients * SENT_COUNT),
      dp("vertx.http.server.bytesSent[local=127.0.0.1:9195,remote=_]$Total", concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length),
      dp("vertx.http.server.requestCount[code=200,local=127.0.0.1:9195,method=POST,path=/resource,remote=_]$Count", concurrentClients * HTTP_SENT_COUNT));

    List<RegistryInspector.Datapoint> timersDp = RegistryInspector.listTimers("vertx.http.server.", registryName)
      .stream().filter(dp -> dp.id().startsWith("vertx.http.server.")).collect(Collectors.toList());
    assertThat(timersDp).extracting(RegistryInspector.Datapoint::id).containsOnly(
      "vertx.http.server.responseTime[local=127.0.0.1:9195,path=/resource,remote=_]$TotalTime",
      "vertx.http.server.responseTime[local=127.0.0.1:9195,path=/resource,remote=_]$Count",
      "vertx.http.server.responseTime[local=127.0.0.1:9195,path=/resource,remote=_]$Max");
  }

  private void runClientRequests(TestContext ctx, boolean ws) throws InterruptedException {
    Async clientsFinished = ctx.async(concurrentClients);
    for (int i = 0; i < concurrentClients; i++) {
      ForkJoinPool.commonPool().execute(() -> {
        HttpClient httpClient = vertx.createHttpClient();
        createdClients.add(httpClient);
        httpRequest(httpClient, ctx);
        if (ws) {
          wsRequest(httpClient, ctx);
        }
        clientsFinished.countDown();
      });
    }
    clientsFinished.awaitSuccess();
  }

  private void httpRequest(HttpClient httpClient, TestContext ctx) {
    Async async = ctx.async(HTTP_SENT_COUNT);
    for (int i = 0; i < HTTP_SENT_COUNT; i++) {
      httpClient.post(9195, "127.0.0.1", "/resource", response -> {
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

  private void wsRequest(HttpClient httpClient, TestContext ctx) {
    Async async = ctx.async();
    httpClient.websocket(9195, "127.0.0.1", "", ws -> {
      ws.handler(event -> {
        async.complete();
        ws.close();
      });
      ws.writeTextMessage(CLIENT_REQUEST);
    }, ctx::fail);
    async.await();
  }
}
