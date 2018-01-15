package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.Comparators;
import io.vertx.ext.monitoring.collector.DummyVertxMetrics;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class NamedPoolTest {

  private Object watcherRef;

  @After
  public void teardown() {
    if (watcherRef != null) {
      DummyVertxMetrics.REPORTER.remove(watcherRef);
    }
  }

  @Test
  public void shouldReportNamedPoolMetrics(TestContext context) throws InterruptedException {
    int maxPoolSize = 8;
    int taskCount = maxPoolSize * 3;
    int sleepMillis = 30;
    Async assertions = context.async();

    Map<String, Comparator<Number>> specialComparators = new HashMap<>(2);
    int margin = 500;
    // If all tasks could be submitted *exactly* at the same time, cumulated delay would be maxPoolSize * 3 * sleepMillis
    // In practice, the value will be close, but not equal
    // So let's make sure it's at least maxPoolSize * 2 * sleepMillis and no greater than maxPoolSize * 3 * sleepMillis + margin
    specialComparators.put("vertx.pool.worker.test-worker.delay", (actual, expected) ->
      (actual.longValue() >= expected.longValue() && actual.longValue() <= expected.longValue() + (maxPoolSize * sleepMillis) + margin
        ? 0 : -1));
    specialComparators.put("vertx.pool.worker.test-worker.usage", (actual, expected) ->
      (actual.longValue() >= expected.longValue() && actual.longValue() <= expected.longValue() + margin
        ? 0 : -1));

    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.pool.worker.test-worker"), // filter
      dp -> dp.getName().equals("vertx.pool.worker.test-worker.completed") && (long) (dp.getValue()) > 0, // wait until
      dataPoints -> {
        // Discard watch if there's no completed task yet
        context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
          // We use a special comparator for processingTime: must be >= expected and <= expected+100ms
          .usingElementComparator(Comparators.metricValueComparators(specialComparators))
          .containsOnly(
            tuple("vertx.pool.worker.test-worker.delay", maxPoolSize * 2 * sleepMillis),
            tuple("vertx.pool.worker.test-worker.queued", 0.0),
            tuple("vertx.pool.worker.test-worker.queuedCount", (long) taskCount),
            tuple("vertx.pool.worker.test-worker.usage", taskCount * sleepMillis),
            tuple("vertx.pool.worker.test-worker.inUse", 0.0),
            tuple("vertx.pool.worker.test-worker.completed", (long) taskCount),
            tuple("vertx.pool.worker.test-worker.maxPoolSize", (double) maxPoolSize),
            tuple("vertx.pool.worker.test-worker.poolRatio", 0.0)));
        assertions.complete();
      },
      context::fail);

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new BatchingReporterOptions()
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
  }
}
