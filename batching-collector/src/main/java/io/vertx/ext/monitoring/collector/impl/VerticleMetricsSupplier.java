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

import io.vertx.core.Verticle;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.*;

/**
 * @author Thomas Segismont
 */
public class VerticleMetricsSupplier implements MetricSupplier {
  private final ConcurrentMap<String, Integer> verticleInstances = new ConcurrentHashMap<>();
  private final String baseName;

  public VerticleMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.verticle.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();
    return verticleInstances.entrySet().stream()
      .map(entry -> new GaugePoint(entry.getKey(), timestamp, entry.getValue().doubleValue()))
      .collect(toList());
  }

  public void verticleDeployed(Verticle verticle) {
    verticleInstances.compute(nameOf(verticle), (name, val) -> val == null ? 1 : val + 1);
  }

  private String nameOf(Verticle verticle) {
    return baseName + verticle.getClass().getName();
  }

  public void verticleUndeployed(Verticle verticle) {
    verticleInstances.compute(nameOf(verticle), (name, val) -> (val == null) ? null : ((val > 1) ? (val - 1) : null));
  }
}
