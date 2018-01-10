package io.vertx.monitoring;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
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

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxNetClientServerMetricsTest {
  private static final int SENT_COUNT = 68;
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";

  private final List<NetClient> createdClients = new CopyOnWriteArrayList<>();
  private final int concurrentClients = ForkJoinPool.commonPool().getParallelism();
  private final String registryName = UUID.randomUUID().toString();
  private NetServer netServer;
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
    createdClients.forEach(NetClient::close);
    if (netServer != null) {
      netServer.close();
    }
  }

  @Test
  public void shouldReportNetClientMetrics(TestContext ctx) throws InterruptedException {
    runClientRequests(ctx);

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.net.client.", registryName);
    assertThat(datapoints).containsOnly(
        dp("vertx.net.client.connections[local=?,remote=localhost:9194]$Value", 0),
        dp("vertx.net.client.bytesReceived[local=?,remote=localhost:9194]$Count", concurrentClients * SENT_COUNT),
        dp("vertx.net.client.bytesReceived[local=?,remote=localhost:9194]$Total", concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length),
        dp("vertx.net.client.bytesSent[local=?,remote=localhost:9194]$Count", concurrentClients * SENT_COUNT),
        dp("vertx.net.client.bytesSent[local=?,remote=localhost:9194]$Total", concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length));
  }

  @Test
  public void shouldReportHttpServerMetrics(TestContext ctx) throws InterruptedException {
    runClientRequests(ctx);

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.net.server.", registryName);
    assertThat(datapoints).containsOnly(
      dp("vertx.net.server.connections[local=localhost:9194,remote=_]$Value", 0),
      dp("vertx.net.server.bytesReceived[local=localhost:9194,remote=_]$Count", concurrentClients * SENT_COUNT),
      dp("vertx.net.server.bytesReceived[local=localhost:9194,remote=_]$Total", concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length),
      dp("vertx.net.server.bytesSent[local=localhost:9194,remote=_]$Count", concurrentClients * SENT_COUNT),
      dp("vertx.net.server.bytesSent[local=localhost:9194,remote=_]$Total", concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length));
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
