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
package io.vertx.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.BackendRegistry;
import io.vertx.micrometer.impl.VertxMetricsImpl;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * The micrometer metrics registry.
 *
 * @author Joel Takvorian
 */
public class MicrometerMetricsFactory implements VertxMetricsFactory {

  private static final Map<MeterRegistry, ConcurrentMap<Meter.Id, LongAdder>> longGaugesByRegistry = new WeakHashMap<>(1);

  private final MeterRegistry micrometerRegistry;

  public MicrometerMetricsFactory() {
    this(null);
  }

  /**
   * Build a factory passing the Micrometer MeterRegistry to be used by Vert.x.
   *
   * This is useful in several scenarios, such as:
   * <ul>
   *   <li>if there is already a MeterRegistry used in the application
   * that should be used by Vert.x as well.</li>
   *   <li>to define some backend configuration that is not covered in this module
   * (example: reporting to non-covered backends such as New Relic)</li>
   *   <li>to use Micrometer's CompositeRegistry</li>
   * </ul>
   *
   * This setter is mutually exclusive with setInfluxDbOptions/setPrometheusOptions/setJmxMetricsOptions
   * and takes precedence over them.
   *
   * @param micrometerRegistry the registry to use
   */
  public MicrometerMetricsFactory(MeterRegistry micrometerRegistry) {
    this.micrometerRegistry = micrometerRegistry;
  }

  @Override
  public VertxMetrics metrics(VertxOptions vertxOptions) {
    MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
    MicrometerMetricsOptions options;
    if (metricsOptions instanceof MicrometerMetricsOptions) {
      options = (MicrometerMetricsOptions) metricsOptions;
    } else {
      options = new MicrometerMetricsOptions(metricsOptions.toJson());
    }
    BackendRegistry backendRegistry = BackendRegistries.setupBackend(options, micrometerRegistry);
    ConcurrentMap<Meter.Id, LongAdder> longGauges;
    synchronized (longGaugesByRegistry) {
      longGauges = longGaugesByRegistry.computeIfAbsent(backendRegistry.getMeterRegistry(), meterRegistry -> new ConcurrentHashMap<>());
    }
    VertxMetricsImpl metrics = new VertxMetricsImpl(options, backendRegistry, new LongGauges(longGauges));
    metrics.init();

    return metrics;
  }

  @Override
  public MetricsOptions newOptions(MetricsOptions options) {
    if (options instanceof MicrometerMetricsOptions) {
      return new MicrometerMetricsOptions((MicrometerMetricsOptions) options);
    } else {
      return VertxMetricsFactory.super.newOptions(options);
    }
  }

  @Override
  public MetricsOptions newOptions() {
    return newOptions((JsonObject) null);
  }

  @Override
  public MetricsOptions newOptions(JsonObject jsonObject) {
    return jsonObject == null ? new MicrometerMetricsOptions() : new MicrometerMetricsOptions(jsonObject);
  }
}
