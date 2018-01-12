package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.DummyVertxMetrics;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VerticleTest {

  private Object watcherRef;

  @After
  public void teardown() {
    if (watcherRef != null) {
      DummyVertxMetrics.REPORTER.remove(watcherRef);
    }
  }

  @Test
  public void shouldReportDatagramMetrics(TestContext context) throws InterruptedException {
    String baseName = "vertx.verticle." + SampleVerticle.class.getName();
    AtomicInteger gauge = new AtomicInteger();
    AtomicReference<Async> atomAsync = new AtomicReference<>();
    watcherRef = DummyVertxMetrics.REPORTER.watch(name -> name.startsWith(baseName), dp -> true, dataPoints -> {
      context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName).containsOnly(baseName));
      gauge.set(((Number)dataPoints.get(0).getValue()).intValue());
      atomAsync.get().complete();
    });

    atomAsync.set(context.async());
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions().setEnabled(true)));
    AtomicReference<String> deploymentRef = new AtomicReference<>();
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(3), res -> {
      if (res.succeeded()) {
        deploymentRef.set(res.result());
      } else {
        throw new RuntimeException(res.cause());
      }
    });
    atomAsync.get().awaitSuccess();
    assertThat(gauge.get()).isEqualTo(3);

    atomAsync.set(context.async());
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(4));
    atomAsync.get().awaitSuccess();
    assertThat(gauge.get()).isEqualTo(7);

    atomAsync.set(context.async());
    vertx.undeploy(deploymentRef.get());
    atomAsync.get().awaitSuccess();
    assertThat(gauge.get()).isEqualTo(4);
  }

  private static class SampleVerticle extends AbstractVerticle {}
}
