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
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import io.vertx.core.spi.metrics.PoolMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.POOL_NAME;
import static io.vertx.micrometer.Label.POOL_TYPE;
import static io.vertx.micrometer.MetricsDomain.NAMED_POOLS;

/**
 * @author Joel Takvorian
 */
class VertxPoolMetrics extends AbstractMetrics implements PoolMetrics<Sample, Sample> {

  // Use registered IDs: common tags and meter filters can make different pools share meters.
  // The registry is weakly referenced, and the values do not retain it.
  private static final int PERSISTENT = -1;
  private static final Map<MeterRegistry, Map<Meter.Id, Integer>> referencesByRegistry = new WeakHashMap<>();

  private final Map<Meter.Id, Integer> references;
  private final List<Meter> meters = new ArrayList<>(6);
  private boolean closed;

  static VertxPoolMetrics create(AbstractMetrics parent, String poolType, String poolName, int maxPoolSize) {
    synchronized (parent.registry) {
      return new VertxPoolMetrics(parent, poolType, poolName, maxPoolSize);
    }
  }

  final Timer queueDelay;
  final LongAdder queueSize;
  final Timer usage;
  final LongAdder inUse;
  final LongAdder usageRatio;
  final Counter completed;

  private VertxPoolMetrics(AbstractMetrics parent, String poolType, String poolName, int maxPoolSize) {
    super(parent, NAMED_POOLS);
    synchronized (referencesByRegistry) {
      references = referencesByRegistry.computeIfAbsent(registry, key -> new HashMap<>());
    }
    // HTTP endpoint pools are short-lived; keep their bounded series across connections.
    boolean persistent = "http".equals(poolType);
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
    queueSize = longGaugeBuilder(names.getPoolQueuePending(), LongAdder::doubleValue)
      .description("Number of pending elements in queue")
      .tags(tags)
      .register(registry, meters::add);
    usage = Timer.builder(names.getPoolUsage())
      .description("Time using a resource")
      .tags(tags)
      .register(registry);
    inUse = longGaugeBuilder(names.getPoolInUse(), LongAdder::doubleValue)
      .description("Number of resources used")
      .tags(tags)
      .register(registry, meters::add);
    usageRatio = longGaugeBuilder(names.getPoolUsageRatio(), value -> maxPoolSize > 0 ? value.doubleValue() / maxPoolSize : Double.NaN)
      .description("Pool usage ratio, only present if maximum pool size could be determined")
      .tags(tags)
      .register(registry, meters::add);
    completed = Counter.builder(names.getPoolCompleted())
      .description("Number of elements done with the resource")
      .tags(tags)
      .register(registry);
    meters.add(queueDelay);
    meters.add(usage);
    meters.add(completed);
    for (Meter meter : meters) {
      references.compute(meter.getId(), (id, count) ->
        persistent || (count != null && count == PERSISTENT) ? PERSISTENT : count == null ? 1 : count + 1);
    }
  }

  @Override
  public void close() {
    synchronized (registry) {
      if (closed) {
        return;
      }
      closed = true;
      for (Meter meter : meters) {
        Meter.Id id = meter.getId();
        int count = references.get(id);
        if (count == 1) {
          removeMeter(meter);
          references.remove(id);
        } else if (count > 1) {
          references.put(id, count - 1);
        }
      }
    }
  }

  @Override
  public Sample enqueue() {
    queueSize.increment();
    return Timer.start();
  }

  @Override
  public void dequeue(Sample submitted) {
    queueSize.decrement();
    submitted.stop(queueDelay);
  }

  @Override
  public Sample begin() {
    inUse.increment();
    usageRatio.increment();
    return Timer.start();
  }

  @Override
  public void end(Sample timer) {
    inUse.decrement();
    usageRatio.decrement();
    timer.stop(usage);
    completed.increment();
  }
}
