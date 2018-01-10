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

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.monitoring.backend.BackendRegistries;
import io.vertx.monitoring.backend.BackendRegistry;
import io.vertx.monitoring.match.LabelMatchers;

/**
 * @author Joel Takvorian
 */
public class VertxMetricsFactoryImpl implements VertxMetricsFactory {
  @Override
  public VertxMetrics metrics(Vertx vertx, VertxOptions vertxOptions) {
    MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
    VertxMonitoringOptions options;
    if (metricsOptions instanceof VertxMonitoringOptions) {
      options = (VertxMonitoringOptions) metricsOptions;
    } else {
      options = new VertxMonitoringOptions(metricsOptions.toJson());
    }
    LabelMatchers labelMatchers = new LabelMatchers(options.getLabelMatchs());
    BackendRegistry backendRegistry = BackendRegistries.setupBackend(vertx, options);
    return new VertxMetricsImpl(options, labelMatchers, backendRegistry);
  }

  @Override
  public MetricsOptions newOptions() {
    return new VertxMonitoringOptions();
  }
}
