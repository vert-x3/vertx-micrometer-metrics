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

package io.vertx.ext.monitoring.influxdb.impl;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.monitoring.influxdb.VertxInfluxDbOptions;
import io.vertx.ext.monitoring.collector.Reporter;
import io.vertx.ext.monitoring.collector.impl.BatchingVertxMetrics;

/**
 * Metrics SPI implementation for InfluxDb.
 *
 * @author Dan Kristensen
 */
public class InfluxDbVertxMetrics extends BatchingVertxMetrics<VertxInfluxDbOptions> {

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx InfluxDb options
   */
  public InfluxDbVertxMetrics(Vertx vertx, VertxInfluxDbOptions options) {
    super(vertx, options);
  }

  @Override
  public Reporter createReporter(Context context) {
    return new InfluxDbReporter(vertx, options, context);
  }
}
