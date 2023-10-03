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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.POOL_NAME;
import static io.vertx.micrometer.Label.POOL_TYPE;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;
import static java.util.function.Function.identity;

/**
 * @author Joel Takvorian
 */
class VertxPoolMetrics extends AbstractMetrics {

  VertxPoolMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, MetricsDomain.NAMED_POOLS, longGauges, enabledLabels);
  }

  PoolMetrics<Timer.Sample> forInstance(String poolType, String poolName, int maxPoolSize) {
    return new Instance(poolType, poolName, maxPoolSize);
  }

  class Instance implements MicrometerMetrics, PoolMetrics<Timer.Sample> {

    final Timer queueDelay;
    final LongAdder queueSize;
    final Timer usage;
    final LongAdder inUse;
    final LongAdder usageRatio;
    final Counter completed;

    Instance(String poolType, String poolName, int maxPoolSize) {
      TagsWrapper tags = of(toTag(POOL_TYPE, identity(), poolType), toTag(POOL_NAME, identity(), poolName));
      queueDelay = timer(names.getPoolQueueTime())
        .description("Time spent in queue before being processed")
        .tags(tags.unwrap())
        .register(registry);
      queueSize = longGauge(names.getPoolQueuePending())
        .description("Number of pending elements in queue")
        .tags(tags.unwrap())
        .register(registry);
      usage = timer(names.getPoolUsage())
        .description("Time using a resource")
        .tags(tags.unwrap())
        .register(registry);
      inUse = longGauge(names.getPoolInUse())
        .description("Number of resources used")
        .tags(tags.unwrap())
        .register(registry);
      usageRatio = longGauge(names.getPoolUsageRatio(), value -> maxPoolSize > 0 ? value.doubleValue() / maxPoolSize : Double.NaN)
        .description("Pool usage ratio, only present if maximum pool size could be determined")
        .tags(tags.unwrap())
        .register(registry);
      completed = counter(names.getPoolCompleted())
        .description("Number of elements done with the resource")
        .tags(tags.unwrap())
        .register(registry);
    }

    @Override
    public Timer.Sample submitted() {
      queueSize.increment();
      return Timer.start();
    }

    @Override
    public void rejected(Timer.Sample submitted) {
      queueSize.decrement();
      submitted.stop(queueDelay);
    }

    @Override
    public Timer.Sample begin(Timer.Sample submitted) {
      queueSize.decrement();
      submitted.stop(queueDelay);
      inUse.increment();
      usageRatio.increment();
      return Timer.start();
    }

    @Override
    public void end(Timer.Sample timer, boolean succeeded) {
      inUse.decrement();
      usageRatio.decrement();
      timer.stop(usage);
      completed.increment();
    }

    @Override
    public MeterRegistry registry() {
      return registry;
    }

    @Override
    public String baseName() {
      return VertxPoolMetrics.this.baseName();
    }
  }
}
