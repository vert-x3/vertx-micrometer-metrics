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

import io.vertx.core.spi.metrics.PoolMetrics;

import java.util.concurrent.atomic.LongAdder;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author Thomas Segismont
 */
public class PoolMetricsImpl implements PoolMetrics<Long> {
  private final NamedPoolMetricsSupplier namedPoolMetricsSupplier;
  private final String poolType;
  private final String poolName;
  private final int maxPoolSize;

  private final LongAdder delay = new LongAdder();
  private final LongAdder queued = new LongAdder();
  private final LongAdder queuedCount = new LongAdder();
  private final LongAdder usage = new LongAdder();
  private final LongAdder inUse = new LongAdder();
  private final LongAdder completed = new LongAdder();

  public PoolMetricsImpl(NamedPoolMetricsSupplier namedPoolMetricsSupplier, String poolType, String poolName, int maxPoolSize) {
    this.namedPoolMetricsSupplier = namedPoolMetricsSupplier;
    this.poolType = poolType;
    this.poolName = poolName;
    this.maxPoolSize = maxPoolSize;
    namedPoolMetricsSupplier.register(this);
  }

  @Override
  public Long submitted() {
    queued.increment();
    queuedCount.increment();
    return System.nanoTime();
  }

  @Override
  public void rejected(Long submitted) {
    queued.decrement();
  }

  @Override
  public Long begin(Long submitted) {
    long time = System.nanoTime();
    delay.add(time - submitted);
    queued.decrement();
    inUse.increment();
    return time;
  }

  @Override
  public void end(Long begin, boolean succeeded) {
    usage.add(System.nanoTime() - begin);
    completed.increment();
    inUse.decrement();
  }

  /**
   * @return the underlying pool type
   */
  public String getPoolType() {
    return poolType;
  }

  /**
   * @return the underlying pool name
   */
  public String getPoolName() {
    return poolName;
  }

  /**
   * @return the underlying pool maximum size
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * @return cumulated queue delay
   */
  public long getDelay() {
    return MILLISECONDS.convert(delay.sum(), NANOSECONDS);
  }

  /**
   * @return current number of elements in the queue
   */
  public long getQueued() {
    return queued.sum();
  }

  /**
   * @return total number of elements queued
   */
  public long getQueuedCount() {
    return queuedCount.sum();
  }

  /**
   * @return cumulated usage (processing) time
   */
  public long getUsage() {
    return MILLISECONDS.convert(usage.sum(), NANOSECONDS);
  }

  /**
   * @return current number of resources used
   */
  public long getInUse() {
    return inUse.sum();
  }

  /**
   * @return total number of elements (tasks) completed
   */
  public long getCompleted() {
    return completed.sum();
  }

  /**
   * @return underlying pool usage ratio; negative if maximum pool size is unknown
   */
  public double getUsageRatio() {
    return maxPoolSize > 0 ? ((double) getInUse()) / maxPoolSize : -1;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    namedPoolMetricsSupplier.unregister(this);
  }
}
