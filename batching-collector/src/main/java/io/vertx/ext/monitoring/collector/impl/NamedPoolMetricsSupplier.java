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

package io.vertx.ext.monitoring.collector.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * Aggregates values from {@link PoolMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class NamedPoolMetricsSupplier implements MetricSupplier {
  private final Set<PoolMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();
  private final String baseName;

  public NamedPoolMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.pool.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();
    return metricsSet.stream()
      .flatMap(poolMetrics -> {
        String base = baseName + poolMetrics.getPoolType() + "." + poolMetrics.getPoolName() + ".";
        Stream.Builder<DataPoint> dataPoints = Stream.<DataPoint>builder()
          .add(new CounterPoint(base + "delay", timestamp, poolMetrics.getDelay()))
          .add(new GaugePoint(base + "queued", timestamp, poolMetrics.getQueued()))
          .add(new CounterPoint(base + "queuedCount", timestamp, poolMetrics.getQueuedCount()))
          .add(new CounterPoint(base + "usage", timestamp, poolMetrics.getUsage()))
          .add(new GaugePoint(base + "inUse", timestamp, poolMetrics.getInUse()))
          .add(new CounterPoint(base + "completed", timestamp, poolMetrics.getCompleted()));
        if (poolMetrics.getMaxPoolSize() > 0) {
          dataPoints
            .add(new GaugePoint(base + "maxPoolSize", timestamp, poolMetrics.getMaxPoolSize()))
            .add(new GaugePoint(base + "poolRatio", timestamp, poolMetrics.getUsageRatio()));
        }
        return dataPoints.build();
      })
      .collect(toList());
  }

  public void register(PoolMetricsImpl poolMetrics) {
    metricsSet.add(poolMetrics);
  }

  public void unregister(PoolMetricsImpl poolMetrics) {
    metricsSet.remove(poolMetrics);
  }
}
