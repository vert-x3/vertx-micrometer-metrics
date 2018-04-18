package io.vertx.micrometer;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.backends.BackendRegistries;
import org.assertj.core.util.DoubleComparator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static io.vertx.micrometer.RegistryInspector.dp;
import static io.vertx.micrometer.RegistryInspector.listDatapoints;
import static io.vertx.micrometer.RegistryInspector.startsWith;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxPoolMetricsTest {

  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportNamedPoolMetrics(TestContext context) throws InterruptedException {
    int maxPoolSize = 8;
    int taskCount = maxPoolSize * 3;
    int sleepMillis = 30;

    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
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

    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.pool"));
    assertThat(datapoints).hasSize(10).contains(
      dp("vertx.pool.queue.size[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.inUse[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.ratio[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT", taskCount),
      dp("vertx.pool.queue.delay[pool_name=test-worker,pool_type=worker]$COUNT", taskCount),
      dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$COUNT", taskCount));

    assertThat(datapoints)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(0.1), Double.class)
      .contains(
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$TOTAL_TIME", taskCount * sleepMillis / 1000d),
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$MAX", sleepMillis / 1000d));
  }
}
