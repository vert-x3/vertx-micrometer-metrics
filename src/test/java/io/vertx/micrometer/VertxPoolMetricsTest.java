package io.vertx.micrometer;

import io.vertx.core.WorkerExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.util.DoubleComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxPoolMetricsTest extends MicrometerMetricsTestBase {

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions()
      .addLabels(Label.POOL_NAME);
  }

  @Test
  public void shouldReportNamedPoolMetrics(TestContext context) {
    vertx = vertx(context);

    int maxPoolSize = 8;
    int taskCount = maxPoolSize * 3;
    int sleepMillis = 30;

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
        future.complete();
      }, false, context.asyncAssertSuccess(v -> {
        ready.countDown();
      }));
    }
    ready.awaitSuccess();
    waitForValue(
      context,
      "vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT",
      value -> value.intValue() == taskCount);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.pool"));
    assertThat(datapoints).hasSize(10).contains(
      dp("vertx.pool.queue.pending[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.in.use[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.ratio[pool_name=test-worker,pool_type=worker]$VALUE", 0),
      dp("vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT", taskCount),
      dp("vertx.pool.queue.time[pool_name=test-worker,pool_type=worker]$COUNT", taskCount),
      dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$COUNT", taskCount));

    assertThat(datapoints)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(0.1), Double.class)
      .contains(dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$MAX", sleepMillis / 1000d));

    class GreaterOrEqualsComparator implements Comparator<Double> {
      @Override
      public int compare(Double o1, Double o2) {
        return o1 < o2 ? -1 : 0;
      }
    }

    assertThat(datapoints)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new GreaterOrEqualsComparator(), Double.class)
      .contains(
        dp("vertx.pool.usage[pool_name=test-worker,pool_type=worker]$TOTAL_TIME", taskCount * sleepMillis / 1000d));
  }
}
