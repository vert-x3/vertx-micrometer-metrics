/*
 * Copyright (c) 2011-2022 The original author or authors
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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.ConcurrentMap;
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

  VertxPoolMetrics(MeterRegistry registry, MetricsNaming names, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.NAMED_POOLS, gaugesTable);
    queueDelay = timers(names.getPoolQueueTime(), "Time spent in queue before being processed", Label.POOL_TYPE, Label.POOL_NAME);
    queueSize = longGauges(names.getPoolQueuePending(), "Number of pending elements in queue", Label.POOL_TYPE, Label.POOL_NAME);
    usage = timers(names.getPoolUsage(), "Time using a resource", Label.POOL_TYPE, Label.POOL_NAME);
    inUse = longGauges(names.getPoolInUse(), "Number of resources used", Label.POOL_TYPE, Label.POOL_NAME);
    usageRatio = doubleGauges(names.getPoolUsageRatio(), "Pool usage ratio, only present if maximum pool size could be determined", Label.POOL_TYPE, Label.POOL_NAME);
    completed = counters(names.getPoolCompleted(), "Number of elements done with the resource", Label.POOL_TYPE, Label.POOL_NAME);
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
      queueSize.get(poolType, poolName).increment();
      return queueDelay.start();
    }

    @Override
    public void rejected(Timers.EventTiming submitted) {
      queueSize.get(poolType, poolName).decrement();
      submitted.end(poolType, poolName);
    }

    @Override
    public Timers.EventTiming begin(Timers.EventTiming submitted) {
      queueSize.get(poolType, poolName).decrement();
      submitted.end(poolType, poolName);
      LongAdder l = inUse.get(poolType, poolName);
      l.increment();
      checkRatio(l.longValue());
      return usage.start();
    }

    @Override
    public void end(Timers.EventTiming timer, boolean succeeded) {
      LongAdder l = inUse.get(poolType, poolName);
      l.decrement();
      checkRatio(l.longValue());
      timer.end(poolType, poolName);
      completed.get(poolType, poolName).increment();
    }

    @Override
    public void close() {
    }

    private void checkRatio(long inUse) {
      if (maxPoolSize > 0) {
        usageRatio.get(poolType, poolName)
          .set((double)inUse / maxPoolSize);
      }
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
