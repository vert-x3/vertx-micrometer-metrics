package io.vertx.micrometer;

import io.micrometer.core.instrument.config.MeterFilter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxNetClientServerMetricsTest extends MicrometerMetricsTestBase {
  private static final int SENT_COUNT = 68;
  private static final String SERVER_RESPONSE = "some text";
  private static final String CLIENT_REQUEST = "pitchounette";

  private final int concurrentClients = ForkJoinPool.commonPool().getParallelism();
  private NetServer netServer;

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions()
      .addDisabledMetricsCategory(MetricsDomain.EVENT_BUS)
      .addLabels(Label.LOCAL, Label.REMOTE);
  }

  @Override
  protected void setUp(TestContext ctx) {
    super.setUp(ctx);

    vertx = vertx(ctx);

    // Filter out remote labels
    BackendRegistries.getNow(registryName).config().meterFilter(
      MeterFilter.replaceTagValues(Label.REMOTE.toString(), s -> "_", "localhost:9194"));

    // Setup server
    Async serverReady = ctx.async();
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Promise<Void> future) throws Exception {
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

  @Test
  public void shouldReportNetClientMetrics(TestContext ctx) {
    runClientRequests(ctx);

    waitForValue(ctx, "vertx.net.client.bytes.read[local=?,remote=localhost:9194]$COUNT",
      value -> value.intValue() == concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.net.client."));
    assertThat(datapoints).containsOnly(
        dp("vertx.net.client.active.connections[local=?,remote=localhost:9194]$VALUE", 0),
        dp("vertx.net.client.bytes.read[local=?,remote=localhost:9194]$COUNT", concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length),
        dp("vertx.net.client.bytes.written[local=?,remote=localhost:9194]$COUNT", concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length));
  }

  @Test
  public void shouldReportNetServerMetrics(TestContext ctx) {
    runClientRequests(ctx);

    waitForValue(ctx, "vertx.net.server.bytes.read[local=localhost:9194,remote=_]$COUNT",
      value -> value.intValue() == concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.net.server."));
    assertThat(datapoints).containsOnly(
      dp("vertx.net.server.active.connections[local=localhost:9194,remote=_]$VALUE", 0),
      dp("vertx.net.server.bytes.read[local=localhost:9194,remote=_]$COUNT", concurrentClients * SENT_COUNT * CLIENT_REQUEST.getBytes().length),
      dp("vertx.net.server.bytes.written[local=localhost:9194,remote=_]$COUNT", concurrentClients * SENT_COUNT * SERVER_RESPONSE.getBytes().length));
  }

  private void runClientRequests(TestContext ctx) {
    Async clientsFinished = ctx.async(concurrentClients);
    for (int i = 0; i < concurrentClients; i++) {
      ForkJoinPool.commonPool().execute(() -> {
        NetClient client = vertx.createNetClient();
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
