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
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.POOL_NAME;
import static io.vertx.micrometer.Label.POOL_TYPE;
import static io.vertx.micrometer.MetricsDomain.NAMED_POOLS;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;
import static java.util.function.Function.identity;

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
    TagsWrapper tags = of(toTag(POOL_TYPE, identity(), poolType), toTag(POOL_NAME, identity(), poolName));
    queueDelay = timer(names.getPoolQueueTime(), "Time spent in queue before being processed", tags.unwrap());
    queueSize = longGauge(names.getPoolQueuePending(), "Number of pending elements in queue", tags.unwrap());
    usage = timer(names.getPoolUsage(), "Time using a resource", tags.unwrap());
    inUse = longGauge(names.getPoolInUse(), "Number of resources used", tags.unwrap());
    usageRatio = longGauge(names.getPoolUsageRatio(), "Pool usage ratio, only present if maximum pool size could be determined", tags.unwrap(), value -> maxPoolSize > 0 ? value.doubleValue() / maxPoolSize : Double.NaN);
    completed = counter(names.getPoolCompleted(), "Number of elements done with the resource", tags.unwrap());
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
