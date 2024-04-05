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
class VertxPoolMetrics extends AbstractMetrics implements PoolMetrics<Sample> {

  final Timer queueDelay;
  final LongAdder queueSize;
  final Timer usage;
  final LongAdder inUse;
  final LongAdder usageRatio;
  final Counter completed;

  VertxPoolMetrics(AbstractMetrics parent, String poolType, String poolName, int maxPoolSize) {
    super(parent, NAMED_POOLS);
    Tags tags = Tags.empty();
    if (enabledLabels.contains(POOL_TYPE)) {
      tags = tags.and(POOL_TYPE.toString(), poolType);
    }
    if (enabledLabels.contains(POOL_NAME)) {
      tags = tags.and(POOL_NAME.toString(), poolName);
    }
    queueDelay = Timer.builder(names.getPoolQueueTime())
      .description("Time spent in queue before being processed")
      .tags(tags)
      .register(registry);
    queueSize = longGaugeBuilder(names.getPoolQueuePending(), LongAdder::doubleValue)
      .description("Number of pending elements in queue")
      .tags(tags)
      .register(registry);
    usage = Timer.builder(names.getPoolUsage())
      .description("Time using a resource")
      .tags(tags)
      .register(registry);
    inUse = longGaugeBuilder(names.getPoolInUse(), LongAdder::doubleValue)
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
  public Sample submitted() {
    queueSize.increment();
    return Timer.start();
  }

  @Override
  public void rejected(Sample submitted) {
    queueSize.decrement();
    submitted.stop(queueDelay);
  }

  @Override
  public Sample begin(Sample submitted) {
    queueSize.decrement();
    submitted.stop(queueDelay);
    inUse.increment();
    usageRatio.increment();
    return Timer.start();
  }

  @Override
  public void end(Sample timer, boolean succeeded) {
    inUse.decrement();
    usageRatio.decrement();
    timer.stop(usage);
    completed.increment();
  }
}
