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
package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.BackendRegistry;

import static io.vertx.core.impl.launcher.commands.BareCommand.METRICS_OPTIONS_PROP_PREFIX;

/**
 * @author Joel Takvorian
 */
public class VertxMetricsFactoryImpl implements VertxMetricsFactory {

  public static final String PROMETHEUS_ENABLED = METRICS_OPTIONS_PROP_PREFIX + "prometheus.enabled";
  public static final String PROMETHEUS_PORT = METRICS_OPTIONS_PROP_PREFIX + "prometheus.port";
  public static final int PROMETHEUS_DEFAULT_PORT = 9779;

  public static final String JVM_METRICS_ENABLED = METRICS_OPTIONS_PROP_PREFIX + "jvm.enabled";

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
    VertxMetricsImpl metrics = new VertxMetricsImpl(options, backendRegistry);
    metrics.init();

    if (Boolean.getBoolean(JVM_METRICS_ENABLED)) {
      MeterRegistry registry = BackendRegistries.getDefaultNow();
      new ClassLoaderMetrics().bindTo(registry);
      new JvmMemoryMetrics().bindTo(registry);
      new JvmGcMetrics().bindTo(registry);
      new ProcessorMetrics().bindTo(registry);
      new JvmThreadMetrics().bindTo(registry);
    }

    return metrics;
  }

  @Override
  public MetricsOptions newOptions() {
    MicrometerMetricsOptions options = new MicrometerMetricsOptions();

    if (Boolean.getBoolean(PROMETHEUS_ENABLED)) {
      VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
        .setEnabled(true)
        .setStartEmbeddedServer(true)
        .setEmbeddedServerOptions(new HttpServerOptions().setPort(Integer.getInteger(PROMETHEUS_PORT, PROMETHEUS_DEFAULT_PORT)));
      options.setPrometheusOptions(prometheusOptions);
    }
    return options;
  }
}
