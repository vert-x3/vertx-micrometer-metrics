/*
 * Copyright (c) 2011-2017 The original author or authors
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

package io.vertx.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Counters;
import io.vertx.monitoring.meters.Gauges;
import io.vertx.monitoring.meters.Timers;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxPoolMetrics extends AbstractMetrics {
  private final Timers queueDelay;
  private final Gauges<LongAdder> queueSize;
  private final Timers usage;
  private final Gauges<LongAdder> inUse;
  private final Gauges<AtomicReference<Double>> usageRatio;
  private final Counters completed;

  VertxPoolMetrics(LabelMatchers labelMatchers, MeterRegistry registry) {
    super(labelMatchers, registry, MetricsCategory.NAMED_POOLS, "vertx.pool.");
    queueDelay = timers("queue.delay", "Queue time for a resource", "pool.type", "pool.name");
    queueSize = longGauges("queue.size", "Number of elements waiting for a resource", "pool.type", "pool.name");
    usage = timers("usage", "Time using a resource", "pool.type", "pool.name");
    inUse = longGauges("inUse", "Number of resources used", "pool.type", "pool.name");
    usageRatio = doubleGauges("ratio", "Pool usage ratio, only present if maximum pool size could be determined", "pool.type", "pool.name");
    completed = counters("completed", "Number of elements done with the resource", "pool.type", "pool.name");
  }

  PoolMetrics forInstance(String poolType, String poolName, int maxPoolSize) {
    return new Instance(poolType, poolName, maxPoolSize);
  }

  class Instance implements MicrometerMetrics, PoolMetrics<Timers.EventTiming> {
    private final String poolType;
    private final String poolName;
    private final int maxPoolSize;

    Instance(String poolType, String poolName, int maxPoolSize) {
      this.poolType = poolType;
      this.poolName = poolName;
      this.maxPoolSize = maxPoolSize;
    }

    @Override
    public Timers.EventTiming submitted() {
      queueSize.get(labelMatchers, poolType, poolName).increment();
      return queueDelay.start(labelMatchers, poolType, poolName);
    }

    @Override
    public void rejected(Timers.EventTiming submitted) {
      queueSize.get(labelMatchers, poolType, poolName).decrement();
      submitted.end();
    }

    @Override
    public Timers.EventTiming begin(Timers.EventTiming submitted) {
      queueSize.get(labelMatchers, poolType, poolName).decrement();
      submitted.end();
      LongAdder l = inUse.get(labelMatchers, poolType, poolName);
      l.increment();
      checkRatio(l.longValue());
      return usage.start(labelMatchers, poolType, poolName);
    }

    @Override
    public void end(Timers.EventTiming begin, boolean succeeded) {
      LongAdder l = inUse.get(labelMatchers, poolType, poolName);
      l.decrement();
      checkRatio(l.longValue());
      begin.end();
      completed.get(labelMatchers, poolType, poolName).increment();
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public void close() {
    }

    private void checkRatio(long inUse) {
      if (maxPoolSize > 0) {
        usageRatio.get(labelMatchers, poolType, poolName)
          .set((double)inUse / maxPoolSize);
      }
    }

    @Override
    public MeterRegistry registry() {
      return registry;
    }

    @Override
    public String baseName() {
      return baseName;
    }
  }
}
