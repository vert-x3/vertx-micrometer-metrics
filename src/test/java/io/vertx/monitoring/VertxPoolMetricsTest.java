package io.vertx.monitoring;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.assertj.core.util.DoubleComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxPoolMetricsTest {

  @Test
  public void shouldReportNamedPoolMetrics(TestContext context) throws InterruptedException {
    int maxPoolSize = 8;
    int taskCount = maxPoolSize * 3;
    int sleepMillis = 30;

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new VertxMonitoringOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setEnabled(true)))
      .exceptionHandler(context.exceptionHandler());

    // Setup executor
    WorkerExecutor workerExecutor = vertx.createSharedWorkerExecutor("test-worker", maxPoolSize);
    Async ready = context.async(taskCount);
    for (int i = 0; i < taskCount; i++) {
      workerExecutor.executeBlocking(future -> {
        try {
          Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        ready.countDown();
        future.complete();
      }, false, context.asyncAssertSuccess());
    }
    ready.awaitSuccess();

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.pool.");
    assertThat(datapoints).containsOnly(
      dp("vertx.pool.queue.size[pool.name=test-worker,pool.type=worker]$Value", 0),
      dp("vertx.pool.inUse[pool.name=test-worker,pool.type=worker]$Value", 0),
      dp("vertx.pool.ratio[pool.name=test-worker,pool.type=worker]$Value", 0),
      dp("vertx.pool.completed[pool.name=test-worker,pool.type=worker]$Count", taskCount));

    List<RegistryInspector.Datapoint> timersDp = RegistryInspector.listTimers("vertx.pool.")
      .stream().filter(dp -> dp.id().startsWith("vertx.pool.")).collect(Collectors.toList());
    assertThat(timersDp)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(0.1), Double.class)
      .hasSize(6)
      .contains(
        dp("vertx.pool.queue.delay[pool.name=test-worker,pool.type=worker]$Count", taskCount),
        dp("vertx.pool.usage[pool.name=test-worker,pool.type=worker]$TotalTime", taskCount * sleepMillis / 1000d),
        dp("vertx.pool.usage[pool.name=test-worker,pool.type=worker]$Count", taskCount),
        dp("vertx.pool.usage[pool.name=test-worker,pool.type=worker]$Max", sleepMillis / 1000d));
  }
}
