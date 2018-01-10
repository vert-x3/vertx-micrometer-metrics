package io.vertx.monitoring;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxVerticleMetricsTest {

  @Test
  public void shouldReportVerticleMetrics(TestContext context) throws InterruptedException {
    String metricName = "vertx.verticle[name=" + SampleVerticle.class.getName() + "]";

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new VertxMonitoringOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setEnabled(true)))
      .exceptionHandler(context.exceptionHandler());

    Async async1 = context.async();
    AtomicReference<String> deploymentRef = new AtomicReference<>();
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(3), res -> {
      if (res.succeeded()) {
        deploymentRef.set(res.result());
        async1.complete();
      } else {
        throw new RuntimeException(res.cause());
      }
    });
    async1.awaitSuccess();

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.verticle");
    assertThat(datapoints).containsOnly(
      dp(metricName + "$Value", 3));

    Async async2 = context.async();
    vertx.deployVerticle(SampleVerticle::new, new DeploymentOptions().setInstances(4), res -> {
      if (res.succeeded()) {
        async2.complete();
      } else {
        throw new RuntimeException(res.cause());
      }
    });
    async2.awaitSuccess();

    datapoints = RegistryInspector.listWithoutTimers("vertx.verticle");
    assertThat(datapoints).containsOnly(
      dp(metricName + "$Value", 7));

    Async async3 = context.async();
    vertx.undeploy(deploymentRef.get(), res -> {
      if (res.succeeded()) {
        async3.complete();
      } else {
        throw new RuntimeException(res.cause());
      }
    });
    async3.awaitSuccess();

    datapoints = RegistryInspector.listWithoutTimers("vertx.verticle");
    assertThat(datapoints).containsOnly(
      dp(metricName + "$Value", 4));
  }

  private static class SampleVerticle extends AbstractVerticle {}
}
