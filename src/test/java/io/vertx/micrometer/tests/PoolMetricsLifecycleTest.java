/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.micrometer.tests;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer.Sample;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.impl.VertxMetricsImpl;
import io.vertx.micrometer.impl.meters.LongGauges;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

public class PoolMetricsLifecycleTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final ConcurrentMap<Meter.Id, LongAdder> gauges = new ConcurrentHashMap<>();
  private final MicrometerMetricsOptions options = new MicrometerMetricsOptions()
    .setEnabled(true)
    .setLabels(EnumSet.of(Label.POOL_TYPE, Label.POOL_NAME));

  private VertxMetricsImpl metrics() {
    return new VertxMetricsImpl(options, () -> registry, new LongGauges(gauges));
  }

  @Test
  public void shouldRemoveMetersAndGaugeStateOnClose() {
    PoolMetrics<Sample, Sample> pool = (PoolMetrics<Sample, Sample>) metrics().createPoolMetrics("worker", "test", 4);
    pool.enqueue();
    pool.begin();
    assertThat(registry.getMeters()).hasSize(6);
    assertThat(gauges).hasSize(3);

    pool.close();

    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
    PoolMetrics<?, ?> recreated = metrics().createPoolMetrics("worker", "test", 4);
    assertThat(registry.get("vertx.pool.queue.pending").gauge().value()).isZero();
    assertThat(registry.get("vertx.pool.in.use").gauge().value()).isZero();
    assertThat(registry.get("vertx.pool.ratio").gauge().value()).isZero();
    recreated.close();
  }

  @Test
  public void shouldKeepSharedMetersUntilLastPoolCloses() {
    VertxMetricsImpl metrics = metrics();
    PoolMetrics<?, ?> first = metrics.createPoolMetrics("worker", "test", 4);
    PoolMetrics<?, ?> second = metrics.createPoolMetrics("worker", "test", 4);

    first.close();
    first.close();
    assertThat(registry.getMeters()).hasSize(6);
    assertThat(gauges).hasSize(3);

    second.close();
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldShareMetersWhenLabelsAreDisabled() {
    options.setLabels(EnumSet.noneOf(Label.class));
    VertxMetricsImpl metrics = metrics();
    PoolMetrics<?, ?> first = metrics.createPoolMetrics("worker", "first", 4);
    PoolMetrics<?, ?> second = metrics.createPoolMetrics("redis", "second", 4);

    first.close();
    assertThat(registry.getMeters()).hasSize(6);
    second.close();
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldTrackRegisteredIdsWithCommonTagsAndFilters() {
    registry.config().commonTags(Tags.of("application", "test"));
    // Only gauges share IDs; timers and counters retain distinct pool names.
    registry.config().meterFilter(new MeterFilter() {
      @Override
      public Meter.Id map(Meter.Id id) {
        if (id.getType() == Meter.Type.GAUGE) {
          return id.withTag(io.micrometer.core.instrument.Tag.of("pool_name", "shared"));
        }
        return id;
      }
    });
    VertxMetricsImpl metrics = metrics();
    PoolMetrics<?, ?> first = metrics.createPoolMetrics("worker", "first", 4);
    PoolMetrics<?, ?> second = metrics.createPoolMetrics("worker", "second", 4);
    assertThat(registry.getMeters()).hasSize(9);

    first.close();
    assertThat(registry.getMeters()).hasSize(6);
    assertThat(gauges).hasSize(3);
    second.close();
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldReleaseDeniedGaugeState() {
    registry.config().meterFilter(MeterFilter.deny());
    PoolMetrics<?, ?> pool = metrics().createPoolMetrics("worker", "test", 4);
    pool.close();
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldKeepHttpMetersAcrossEndpointPoolLifetimes() {
    VertxMetricsImpl metrics = metrics();
    PoolMetrics<Sample, Sample> pool =
      (PoolMetrics<Sample, Sample>) metrics.createPoolMetrics("http", "localhost:8080", 4);
    pool.end(pool.begin());
    pool.close();

    PoolMetrics<?, ?> next = metrics.createPoolMetrics("http", "localhost:8080", 4);
    assertThat(registry.getMeters()).hasSize(6);
    assertThat(registry.get("vertx.pool.completed").counter().count()).isEqualTo(1);
    next.close();
    assertThat(registry.getMeters()).hasSize(6);
  }

  @Test
  public void shouldNotAccumulateClosedPools() {
    VertxMetricsImpl metrics = metrics();
    for (int i = 0; i < 100; i++) {
      metrics.createPoolMetrics("redis", "pool-" + i, 4).close();
    }
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldKeepOtherPoolTypesWithSameName() {
    VertxMetricsImpl metrics = metrics();
    PoolMetrics<?, ?> worker = metrics.createPoolMetrics("worker", "test", 4);
    PoolMetrics<?, ?> redis = metrics.createPoolMetrics("redis", "test", 4);
    worker.close();
    assertThat(registry.getMeters()).hasSize(6).allSatisfy(meter ->
      assertThat(meter.getId().getTag("pool_type")).isEqualTo("redis"));
    redis.close();
    assertThat(registry.getMeters()).isEmpty();
    assertThat(gauges).isEmpty();
  }

  @Test
  public void shouldSerializeRegistrationAndLastClose() throws Exception {
    VertxMetricsImpl metrics = metrics();
    ExecutorService executor = Executors.newFixedThreadPool(4);
    CyclicBarrier barrier = new CyclicBarrier(4);
    List<Future<?>> tasks = new ArrayList<>();
    try {
      for (int thread = 0; thread < 4; thread++) {
        tasks.add(executor.submit(() -> {
          for (int i = 0; i < 100; i++) {
            PoolMetrics<?, ?> pool = metrics.createPoolMetrics("worker", "test", 4);
            barrier.await(10, TimeUnit.SECONDS);
            pool.close();
          }
          return null;
        }));
      }
      for (Future<?> task : tasks) {
        task.get(30, TimeUnit.SECONDS);
      }
      assertThat(registry.getMeters()).isEmpty();
      assertThat(gauges).isEmpty();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void shouldShareOwnershipAcrossFactoriesUsingSameRegistry() {
    VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(options);
    VertxMetrics firstMetrics = new MicrometerMetricsFactory(registry).metrics(vertxOptions);
    VertxMetrics secondMetrics = new MicrometerMetricsFactory(registry).metrics(vertxOptions);
    try {
      PoolMetrics<?, ?> first = firstMetrics.createPoolMetrics("worker", "test", 4);
      PoolMetrics<?, ?> second = secondMetrics.createPoolMetrics("worker", "test", 4);
      first.close();
      assertThat(registry.getMeters()).hasSize(6);
      second.close();
      assertThat(registry.getMeters()).isEmpty();
    } finally {
      firstMetrics.close();
      secondMetrics.close();
    }
  }
}
