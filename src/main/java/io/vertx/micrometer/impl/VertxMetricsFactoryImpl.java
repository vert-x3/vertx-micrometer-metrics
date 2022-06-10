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
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.BackendRegistry;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joel Takvorian
 */
public class VertxMetricsFactoryImpl implements VertxMetricsFactory {

  private static final Map<MeterRegistry, ConcurrentHashMap<Meter.Id, Object>> tables = new WeakHashMap<>(1);

  @Override
  public VertxMetrics metrics(VertxOptions vertxOptions) {
    MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
    MicrometerMetricsOptions options;
    if (metricsOptions instanceof MicrometerMetricsOptions) {
      options = (MicrometerMetricsOptions) metricsOptions;
    } else {
      options = new MicrometerMetricsOptions(metricsOptions.toJson());
    }
    BackendRegistry backendRegistry = BackendRegistries.setupBackend(options);
    ConcurrentHashMap<Meter.Id, Object> gaugesTable;
    synchronized (tables) {
      gaugesTable = tables.computeIfAbsent(backendRegistry.getMeterRegistry(), meterRegistry -> new ConcurrentHashMap<>());
    }
    VertxMetricsImpl metrics = new VertxMetricsImpl(options, backendRegistry, gaugesTable);
    metrics.init();

    if (options.isJvmMetricsEnabled()) {
      new ClassLoaderMetrics().bindTo(backendRegistry.getMeterRegistry());
      new JvmMemoryMetrics().bindTo(backendRegistry.getMeterRegistry());
      new JvmGcMetrics().bindTo(backendRegistry.getMeterRegistry());
      new ProcessorMetrics().bindTo(backendRegistry.getMeterRegistry());
      new JvmThreadMetrics().bindTo(backendRegistry.getMeterRegistry());
    }

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
