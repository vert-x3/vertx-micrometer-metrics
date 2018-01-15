package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
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
public class NetClientServerTest {

  private static final int SENT_COUNT = 68;
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";

  private Object watcherRef;
  private final List<NetClient> createdClients = new CopyOnWriteArrayList<>();
  private NetServer netServer;
  private final int concurrentClients = ForkJoinPool.commonPool().getParallelism();
  private final Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new BatchingReporterOptions()
        .setEnabled(true)));

  @Before
  public void setUp(TestContext ctx) {
    // Setup server
    Async serverReady = ctx.async();
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Future<Void> future) throws Exception {
        netServer = vertx.createNetServer();
        netServer
          .connectHandler(socket -> socket.handler(buffer -> socket.write(SERVER_RESPONSE)))
          .listen(9194, "localhost", r -> {
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
    createdClients.forEach(NetClient::close);
    if (netServer != null) {
      netServer.close();
    }
  }

  @Test
  public void shouldReportNetClientMetrics(TestContext ctx) throws InterruptedException {
    Async assertions = ctx.async();

    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.net.client"),  // filter
      dp -> dp.getName().equals("vertx.net.client.localhost:9194.connections") && (double)(dp.getValue()) == 0d,  // wait until
      dataPoints -> {
        ctx.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
          .containsOnly(
            tuple("vertx.net.client.localhost:9194.connections", 0.0),
            tuple("vertx.net.client.localhost:9194.bytesReceived", (long) concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length),
            tuple("vertx.net.client.localhost:9194.bytesSent", (long) concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length),
            tuple("vertx.net.client.localhost:9194.errorCount", 0L)));
        assertions.complete();
      }
    );

    runClientRequests(ctx);
  }

  @Test
  public void shouldReportHttpServerMetrics(TestContext ctx) throws InterruptedException {
    Async assertions = ctx.async();

    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.net.server"),  // filter
      dp -> dp.getName().equals("vertx.net.server.localhost:9194.connections") && (double)(dp.getValue()) == 0d,  // wait until
      dataPoints -> {
        ctx.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue).containsOnly(
          tuple("vertx.net.server.localhost:9194.connections", 0.0),
          tuple("vertx.net.server.localhost:9194.bytesReceived", (long) concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length),
          tuple("vertx.net.server.localhost:9194.bytesSent", (long) concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length),
          tuple("vertx.net.server.localhost:9194.errorCount", 0L)));
        assertions.complete();
      }
    );

    runClientRequests(ctx);
  }

  private void runClientRequests(TestContext ctx) {
    Async clientsFinished = ctx.async(concurrentClients);
    for (int i = 0; i < concurrentClients; i++) {
      ForkJoinPool.commonPool().execute(() -> {
        NetClient client = vertx.createNetClient();
        createdClients.add(client);
        request(client, ctx);
        clientsFinished.countDown();
      });
    }
    clientsFinished.awaitSuccess();
  }

  private void request(NetClient client, TestContext ctx) {
    for (int i = 0; i < SENT_COUNT; i++) {
      Async async = ctx.async();
      client.connect(9194, "localhost", res -> {
        if (res.failed()) {
          async.complete();
          ctx.fail(res.cause());
          return;
        }
        NetSocket socket = res.result().exceptionHandler(t -> {
          async.complete();
          ctx.fail(t);
        });
        socket.handler(buf -> socket.close());
        socket.write(CLIENT_REQUEST);
        socket.closeHandler(v -> async.complete());
      });
      async.await();
    }
  }
}
