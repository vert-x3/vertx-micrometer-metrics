package io.vertx.micrometer.tests;

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
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class UnixSocketTest extends MicrometerMetricsTestBase {

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions()
      .addDisabledMetricsCategory(MetricsDomain.EVENT_BUS)
      .addLabels(Label.REMOTE);
  }

  @Override
  protected Vertx vertx(TestContext context) {
    return Vertx.vertx(new VertxOptions().setPreferNativeTransport(true).setMetricsOptions(metricsOptions))
      .exceptionHandler(context.exceptionHandler());
  }

  @Test
  public void shouldWriteOnUnixSocket(TestContext ctx) {
    vertx = vertx(ctx);

    Async allDeployed = ctx.async();
    vertx.deployVerticle(
      new DomainSocketServer()).onComplete(
      h -> vertx.deployVerticle(new DomainSocketClientTriggerVerticle()).onComplete( ch -> allDeployed.complete()));

    allDeployed.await(2000);
    waitForValue(ctx, "vertx.net.client.active.connections[remote=/var/tmp/myservice.sock]$VALUE", v -> v.intValue() == 0);
    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.net.client."));
    assertThat(datapoints).contains(
      dp("vertx.net.client.active.connections[remote=/var/tmp/myservice.sock]$VALUE", 0),
      dp("vertx.net.client.bytes.written[remote=/var/tmp/myservice.sock]$COUNT", 4));
  }

  public static class DomainSocketServer extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      vertx.createHttpServer().requestHandler(req -> {
        })
        .listen(SocketAddress.domainSocketAddress("/var/tmp/myservice.sock")).onComplete(ar -> {
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

  public static class DomainSocketClientTriggerVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      NetClient netClient = vertx.createNetClient();
      SocketAddress addr = SocketAddress.domainSocketAddress("/var/tmp/myservice.sock");
      netClient.connect(addr).onComplete(ar -> {
        if (ar.succeeded()) {
          NetSocket socket = ar.result().exceptionHandler(startPromise::fail);
          socket.write("test");
          socket.close().onComplete(v -> startPromise.complete());
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
