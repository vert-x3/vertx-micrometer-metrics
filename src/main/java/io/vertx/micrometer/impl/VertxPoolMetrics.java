/*
 * Copyright (c) 2011-2023 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import io.vertx.core.spi.metrics.PoolMetrics;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.POOL_NAME;
import static io.vertx.micrometer.Label.POOL_TYPE;
import static io.vertx.micrometer.MetricsDomain.NAMED_POOLS;

/**
 * @author Joel Takvorian
 */
class VertxPoolMetrics extends AbstractMetrics implements PoolMetrics<VertxPoolMetrics.QueueMetric, VertxPoolMetrics.UsageMetric> {

  final Timer queueDelay;
  final LongTaskTimer queueSize;
  final Timer usage;
  final LongTaskTimer inUse;
  final LongAdder usageRatio;
  final Counter completed;

  VertxPoolMetrics(AbstractMetrics parent, String poolType, String poolName, int maxPoolSize) {
    super(parent, NAMED_POOLS);
    Tags tags = Tags.empty();
    if (enabledLabels.contains(POOL_TYPE) || "http".equals(poolType)) {
      tags = tags.and(POOL_TYPE.toString(), poolType);
    }
    if (enabledLabels.contains(POOL_NAME) || "http".equals(poolType)) {
      tags = tags.and(POOL_NAME.toString(), poolName);
    }
    queueDelay = Timer.builder(names.getPoolQueueTime())
      .description("Time spent in queue before being processed")
      .tags(tags)
      .register(registry);
    queueSize = LongTaskTimer.builder(names.getPoolQueuePending())
      .description("Number of pending elements in queue")
      .tags(tags)
      .register(registry);
    usage = Timer.builder(names.getPoolUsage())
      .description("Time using a resource")
      .tags(tags)
      .register(registry);
    inUse = LongTaskTimer.builder(names.getPoolInUse())
      .description("Number of resources used")
      .tags(tags)
      .register(registry);
    usageRatio = longGaugeBuilder(names.getPoolUsageRatio(), value -> maxPoolSize > 0 ? value.doubleValue() / maxPoolSize : Double.NaN)
      .description("Pool usage ratio, only present if maximum pool size could be determined")
      .tags(tags)
      .register(registry);
    completed = Counter.builder(names.getPoolCompleted())
      .description("Number of elements done with the resource")
      .tags(tags)
      .register(registry);
  }

  @Override
  public QueueMetric enqueue() {
    return new QueueMetric(queueDelay, queueSize);
  }

  @Override
  public void dequeue(QueueMetric queueMetric) {
    queueMetric.dequeue();
  }

  @Override
  public UsageMetric begin() {
    usageRatio.increment();
    return new UsageMetric(usage, inUse);
  }

  @Override
  public void end(UsageMetric usageMetric) {
    usageMetric.end();
    usageRatio.decrement();
    completed.increment();
  }

  static class QueueMetric {
    private final Timer queueDelay;
    private final LongTaskTimer.Sample queueSize;
    private final Sample submitted;

    private QueueMetric(Timer queueDelay, LongTaskTimer queueSize) {
      this.queueDelay = queueDelay;
      this.queueSize = queueSize.start();
      this.submitted = Timer.start();
    }

    private void dequeue() {
      queueSize.stop();
      submitted.stop(queueDelay);
    }
  }

  static class UsageMetric {
    private final Timer usage;
    private final LongTaskTimer.Sample inUse;
    private final Sample begun;

    private UsageMetric(Timer usage, LongTaskTimer inUse) {
      this.usage = usage;
      this.inUse = inUse.start();
      begun = Timer.start();
    }

    private void end() {
      inUse.stop();
      begun.stop(usage);
    }
  }
}
