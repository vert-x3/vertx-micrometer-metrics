/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.micrometer.tests;

import io.vertx.core.WorkerExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.util.DoubleComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxPoolMetricsTest extends MicrometerMetricsTestBase {

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
      workerExecutor.executeBlocking(() -> {
        Thread.sleep(sleepMillis);
        return null;
      }, false).onComplete(context.asyncAssertSuccess(v -> {
        ready.countDown();
      }));
    }
    ready.awaitSuccess();
    waitForValue(
      context,
      "vertx.pool.completed[pool_name=test-worker,pool_type=worker]$COUNT",
      value -> value.intValue() == taskCount);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.pool").and(hasTag("pool_name", "test-worker")));
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

  @Test
  public void shouldReportUsageMetrics(TestContext context) {
    vertx = vertx(context);

    int maxPoolSize = 8;
    int taskCount = maxPoolSize * 3;
    CountDownLatch latch = new CountDownLatch(1);

    Async ready = context.async(taskCount);
    WorkerExecutor workerExecutor = vertx.createSharedWorkerExecutor("test-worker", maxPoolSize);
    for (int i = 0; i < taskCount; i++) {
      workerExecutor.executeBlocking(() -> {
        latch.await();
        return null;
      }, false).onComplete(context.asyncAssertSuccess(v -> {
        ready.countDown();
      }));
    }

    waitForValue(
      context,
      "vertx.pool.in.use[pool_name=test-worker,pool_type=worker]$VALUE",
      value -> value.intValue() == maxPoolSize);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.pool.ratio").and(hasTag("pool_name", "test-worker")));
    assertThat(datapoints).hasSize(1).contains(
      dp("vertx.pool.ratio[pool_name=test-worker,pool_type=worker]$VALUE", 1.0D));

    latch.countDown();
    ready.awaitSuccess();
  }
}
