package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.Comparators;
import io.vertx.ext.monitoring.collector.DummyVertxMetrics;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class HttpClientTest {

  private static final int SENT_COUNT = 68;

  private Object observerRef;
  private final List<HttpClient> createdClients = new ArrayList<>();

  @After
  public void teardown() {
    if (observerRef != null) {
      DummyVertxMetrics.REPORTER.remove(observerRef);
    }
    createdClients.forEach(HttpClient::close);
  }

  @Test
  public void shouldReportHttpClientMetrics(TestContext ctx) throws InterruptedException {
    Async assertions = ctx.async();
    int concurrentClients = ForkJoinPool.commonPool().getParallelism();
    long reqDelay = 11L;
    long expectedRequestCount = concurrentClients * SENT_COUNT;
    long expectedRequestDelay = expectedRequestCount * reqDelay;

    observerRef = DummyVertxMetrics.REPORTER.observe(name -> name.startsWith("vertx.http.client"), dataPoints -> {
      ctx.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
        // We use a special comparator for responseTime: must be >= expected and <= expected+margin
        .usingElementComparator(Comparators.metricValueComparator("vertx.http.client.127.0.0.1:9195.responseTime",
          (actual, expected) ->
            (actual.longValue() >= expected.longValue() && actual.longValue() <= expected.longValue() + 5 * expectedRequestCount)
              ? 0 : -1))
        .hasSize(8)
        .contains(
          tuple("vertx.http.client.127.0.0.1:9195.wsConnections", 0.0),
          tuple("vertx.http.client.127.0.0.1:9195.bytesReceived", 4347L),
          tuple("vertx.http.client.127.0.0.1:9195.bytesSent", 5796L),
          tuple("vertx.http.client.127.0.0.1:9195.requestCount", expectedRequestCount),
          tuple("vertx.http.client.127.0.0.1:9195.responseTime", expectedRequestDelay)));
      assertions.complete();
    });

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new BatchingReporterOptions()
        .setSchedule(2)
        .setEnabled(true)));

    // Setup server
    Async serverReady = ctx.async();
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Future<Void> future) throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        httpServer
          .websocketHandler(ws ->
            ws.handler(event -> vertx.setTimer(reqDelay, timer -> ws.writeTextMessage("some text").end())))
          .requestHandler(req ->
            // Timer as artificial processing time
            vertx.setTimer(reqDelay, handler ->
              req.response().setChunked(true).putHeader("Content-Type", "text/plain").write("some text").end()))
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

    Async clientsFinished = ctx.async(concurrentClients);
    for (int i = 0; i < concurrentClients; i++) {
      ForkJoinPool.commonPool().execute(() -> {
        HttpClient httpClient = vertx.createHttpClient();
        createdClients.add(httpClient);
        httpRequest(httpClient, ctx);
        wsRequest(httpClient, ctx);
        clientsFinished.countDown();
      });
    }
    clientsFinished.awaitSuccess();

    assertions.awaitSuccess();
  }

  private void httpRequest(HttpClient httpClient, TestContext ctx) {
    String content = "pitchounette";
    for (int i = 0; i < SENT_COUNT; i++) {
      Async async = ctx.async();
      httpClient.post(9195, "127.0.0.1", "", response -> {
        async.complete();
        if (response.statusCode() != 200) {
          ctx.fail(response.statusMessage());
        }
      }).exceptionHandler(t -> {
        async.complete();
        ctx.fail(t);
      }).putHeader("Content-Length", String.valueOf(content.getBytes().length))
        .write(content)
        .end();
      async.await();
    }
  }

  private void wsRequest(HttpClient httpClient, TestContext ctx) {
    String content = "pitchounette";
    Async async = ctx.async();
    httpClient.websocket(9195, "127.0.0.1", "", ws -> {
      ws.handler(event -> {
        async.complete();
        ws.close();
      });
      ws.writeTextMessage(content);
    }, ctx::fail);
    async.await();
  }
}
