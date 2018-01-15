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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VerticleTest {

  @Test
  public void shouldReportVerticleMetrics(TestContext context) throws InterruptedException {
    String metricName = "vertx.verticle." + SampleVerticle.class.getName();

    Async async1 = context.async();
    DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith(metricName),
      dp -> true,
      dataPoints -> {
        try {
          context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
            .containsOnly(tuple(metricName, 3.0)));
        } finally {
          async1.complete();
        }
      },
      context::fail);

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions().setEnabled(true)))
      .exceptionHandler(context.exceptionHandler());
    AtomicReference<String> deploymentRef = new AtomicReference<>();
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(3), res -> {
      if (res.succeeded()) {
        deploymentRef.set(res.result());
      } else {
        throw new RuntimeException(res.cause());
      }
    });
    async1.awaitSuccess();

    Async async2 = context.async();
    DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith(metricName),
      dp -> true,
      dataPoints -> {
        try {
          context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
            .containsOnly(tuple(metricName, 7.0)));
        } finally {
          async2.complete();
        }
      },
      context::fail);
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(4));
    async2.awaitSuccess();

    Async async3 = context.async();
    DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith(metricName),
      dp -> true,
      dataPoints -> {
        try {
          context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
            .containsOnly(tuple(metricName, 4.0)));
        } finally {
          async3.complete();
        }
      },
      context::fail);
    vertx.undeploy(deploymentRef.get());
    async3.awaitSuccess();
  }

  private static class SampleVerticle extends AbstractVerticle {}
}
