/*
 * Copyright (c) 2011-2014 The original author or authors
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

package io.vertx.monitoring.service;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.Measured;

import java.util.Set;

/**
 * The metrics service mainly allows to return a snapshot of measured objects.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @author Joel Takvorian
 */
@VertxGen
public interface MetricsService {

  /**
   * Creates a metric service for a given {@link Measured} object.
   *
   * @param measured the measured object
   * @return the metrics service
   */
  static MetricsService create(Measured measured) {
    return new MetricsServiceImpl(measured);
  }

  /**
   * @return the base name of the measured object
   */
  String getBaseName();

  /**
   * @return the known metrics names by this service
   */
  Set<String> metricsNames();

  /**
   * Will return the metrics that correspond with the {@code measured} object, null if no metrics is available.<p/>
   *
   * Note: in the case of scaled servers, the JsonObject returns an aggregation of the metrics as the
   * dropwizard backend reports to a single server.
   *
   * @return the map of metrics where the key is the name of the metric (excluding the base name unless for the Vert.x object)
   * and the value is the json data representing that metric
   */
  JsonObject getMetricsSnapshot();

  /**
   * Will return the metrics that begins with the {@code baseName}, null if no metrics is available.<p/>
   *
   * Note: in the case of scaled servers, the JsonObject returns an aggregation of the metrics as the
   * dropwizard backend reports to a single server.
   *
   * @return the map of metrics where the key is the name of the metric and the value is the json data
   * representing that metric
   */
  JsonObject getMetricsSnapshot(String baseName);
}
