package io.vertx.micrometer;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.util.DoubleComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.micrometer.RegistryInspector.dp;
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

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
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
    RegistryInspector.waitForValue(
      vertx,
      context,
      "vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT",
      value -> value.intValue() == taskCount);

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.pool.");
    assertThat(datapoints).containsOnly(
      dp("vertx.pool.queue.size[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.inUse[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.ratio[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT", taskCount));

    List<RegistryInspector.Datapoint> timersDp = RegistryInspector.listTimers("vertx.pool.")
      .stream().filter(dp -> dp.id().startsWith("vertx.pool.")).collect(Collectors.toList());
    assertThat(timersDp).hasSize(6)
      .contains(
        dp("vertx.pool.queue.delay[pool_name=test-worker,pool_type=worker]$COUNT", taskCount),
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$COUNT", taskCount));

    assertThat(timersDp)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(1.0), Double.class)
      .contains(
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$TOTAL_TIME", taskCount * sleepMillis / 1000d),
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$MAX", sleepMillis / 1000d));
  }
}
