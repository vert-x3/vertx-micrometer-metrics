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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class HttpClientServerTest {

  private static final int SENT_COUNT = 68;
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";
  private static final long REQ_DELAY = 30L;

  private Object watcherRef;
  private final List<HttpClient> createdClients = new CopyOnWriteArrayList<>();
  private HttpServer httpServer;
  private final int concurrentClients = ForkJoinPool.commonPool().getParallelism();
  private final long expectedRequestCount = concurrentClients * SENT_COUNT;
  private final long expectedRequestDelay = expectedRequestCount * REQ_DELAY;
  private Vertx vertx;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions()
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
            ws.handler(event -> vertx.setTimer(REQ_DELAY, timer -> ws.writeTextMessage(SERVER_RESPONSE).end())))
          .requestHandler(req ->
            // Timer as artificial processing time
            vertx.setTimer(REQ_DELAY, handler ->
              req.response().setChunked(true).putHeader("Content-Type", "text/plain").write(SERVER_RESPONSE).end()))
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
    if (watcherRef != null) {
      DummyVertxMetrics.REPORTER.remove(watcherRef);
    }
    createdClients.forEach(HttpClient::close);
    if (httpServer != null) {
      httpServer.close();
    }
  }

  @Test
  public void shouldReportHttpClientMetrics(TestContext ctx) throws InterruptedException {
    Async assertions = ctx.async();
    long waitUntilBytesReceived = (long) concurrentClients * (SENT_COUNT + 1) * SERVER_RESPONSE.getBytes().length;
    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.http.client"), // filter
      dp -> (dp.getName().equals("vertx.http.client.127.0.0.1:9195.bytesReceived")
        && ((long) (dp.getValue())) == waitUntilBytesReceived), // wait until
      dataPoints -> {
        ctx.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
          // We use a special comparator for responseTime: must be >= expected and <= 5*expected (large margin for CI)
          .usingElementComparator(Comparators.metricValueComparator("vertx.http.client.127.0.0.1:9195.responseTime",
            Comparators.atLeast()))
          .hasSize(8)
          .contains(
            tuple("vertx.http.client.127.0.0.1:9195.wsConnections", 0.0),
            tuple("vertx.http.client.127.0.0.1:9195.bytesReceived", waitUntilBytesReceived,
            tuple("vertx.http.client.127.0.0.1:9195.bytesSent", (long) concurrentClients * (SENT_COUNT + 1) * CLIENT_REQUEST.getBytes().length),
            tuple("vertx.http.client.127.0.0.1:9195.requestCount", expectedRequestCount),
            tuple("vertx.http.client.127.0.0.1:9195.responseTime", expectedRequestDelay))));
        assertions.complete();
      },
      ctx::fail
    );
    runClientRequests(ctx);
  }

  @Test
  public void shouldReportHttpServerMetrics(TestContext ctx) throws InterruptedException {
    Async assertions = ctx.async();
    long waitUntilBytesSent = (long) concurrentClients * (SENT_COUNT + 1) * SERVER_RESPONSE.getBytes().length;
    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.http.server"),
      dp -> (dp.getName().equals("vertx.http.server.127.0.0.1:9195.bytesSent")
        && ((long) (dp.getValue())) == waitUntilBytesSent), // wait until
      dataPoints -> {
        ctx.verify(v -> {
          assertThat(dataPoints).extracting(DataPoint::getName).containsOnly(
            "vertx.http.server.127.0.0.1:9195.processingTime",
            "vertx.http.server.127.0.0.1:9195.requestCount",
            "vertx.http.server.127.0.0.1:9195.requests",
            "vertx.http.server.127.0.0.1:9195.httpConnections",
            "vertx.http.server.127.0.0.1:9195.wsConnections",
            "vertx.http.server.127.0.0.1:9195.bytesReceived",
            "vertx.http.server.127.0.0.1:9195.bytesSent",
            "vertx.http.server.127.0.0.1:9195.errorCount");
          assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
            // We use a special comparator for responseTime: must be >= expected and <= 5*expected (large margin for CI)
            .usingElementComparator(Comparators.metricValueComparator("vertx.http.server.127.0.0.1:9195.processingTime",
              Comparators.atLeast()))
            .hasSize(8)
            .contains(
              tuple("vertx.http.server.127.0.0.1:9195.bytesSent", waitUntilBytesSent),
              tuple("vertx.http.server.127.0.0.1:9195.bytesReceived", (long) concurrentClients * (SENT_COUNT + 1) * CLIENT_REQUEST.getBytes().length),
              tuple("vertx.http.server.127.0.0.1:9195.requestCount", expectedRequestCount),
              tuple("vertx.http.server.127.0.0.1:9195.processingTime", expectedRequestDelay));
        });
        assertions.complete();
      },
      ctx::fail
    );
    runClientRequests(ctx);
  }

  private void runClientRequests(TestContext ctx) {
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
  }

  private void httpRequest(HttpClient httpClient, TestContext ctx) {
    Async async = ctx.async(SENT_COUNT);
    for (int i = 0; i < SENT_COUNT; i++) {
      httpClient.post(9195, "127.0.0.1", "", response -> {
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
