package io.vertx.micrometer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;

import static io.vertx.micrometer.RegistryInspector.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class UnixSocketTest {

  private final String registryName = UUID.randomUUID().toString();

  @Test
  public void shouldWriteOnUnixSocket(TestContext ctx) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true)
      .setMetricsOptions(new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .addDisabledMetricsCategory(MetricsDomain.EVENT_BUS)
      .addLabels(Label.REMOTE)
      .setRegistryName(registryName)
      .setEnabled(true)))
      .exceptionHandler(ctx.exceptionHandler());

    Async allDeployed = ctx.async();
    vertx.deployVerticle(
      new DomainSocketServer(),
      h -> vertx.deployVerticle(new DomainSocketClientTriggerVerticle(), ch -> allDeployed.complete()));

    allDeployed.await(2000);
    waitForValue(vertx, ctx, registryName, "vertx.net.client.connections[remote=/var/tmp/myservice.sock]$VALUE", v -> v.intValue() == 0);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(registryName, startsWith("vertx.net.client."));
    assertThat(datapoints).contains(
      dp("vertx.net.client.connections[remote=/var/tmp/myservice.sock]$VALUE", 0),
      dp("vertx.net.client.bytesSent[remote=/var/tmp/myservice.sock]$COUNT", 1),
      dp("vertx.net.client.bytesSent[remote=/var/tmp/myservice.sock]$TOTAL", 4));
  }

  public class DomainSocketServer extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      vertx.createHttpServer().requestHandler(req -> {})
        .listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock"), ar -> {
          if (ar.succeeded()) {
            startPromise.complete();
          } else {
            startPromise.fail(ar.cause());
          }
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
      stopPromise.complete();
    }
  }

  public class DomainSocketClientTriggerVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      NetClient netClient = vertx.createNetClient();
      SocketAddress addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock");
      netClient.connect(addr, ar -> {
        if (ar.succeeded()) {
          NetSocket socket = ar.result().exceptionHandler(startPromise::fail);
          socket.write("test");
          socket.close(v -> startPromise.complete());
        } else {
          startPromise.fail(ar.cause());
        }
      });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
      stopPromise.complete();
    }
  }
}
