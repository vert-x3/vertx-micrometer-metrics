/*
 * Copyright 2023 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGaugeBuilder;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;

/**
 * Abstract class for metrics container.
 *
 * @author Joel Takvorian
 */
public abstract class AbstractMetrics implements MicrometerMetrics {
  protected final MeterRegistry registry;
  protected final MetricsNaming names;
  protected final String category;
  protected final LongGauges longGauges;

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges) {
    this.registry = registry;
    this.names = names;
    this.longGauges = longGauges;
    this.category = null;
  }

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, String category, LongGauges longGauges) {
    this.registry = registry;
    this.names = names;
    this.category = category;
    this.longGauges = longGauges;
  }

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, MetricsDomain domain, LongGauges longGauges) {
    this.registry = registry;
    this.names = names;
    this.category = (domain == null) ? null : domain.toCategory();
    this.longGauges = longGauges;
  }

  /**
   * @return the Micrometer registry used with this measured object.
   */
  @Override
  public MeterRegistry registry() {
    return registry;
  }

  @Override
  public String baseName() {
    return category == null ? null : "vertx." + category + ".";
  }

  Counter.Builder counter(String name) {
    return Counter.builder(baseName() + name);
  }

  LongGaugeBuilder longGauge(String name) {
    return longGauges.builder(baseName() + name, LongAdder::doubleValue);
  }

  LongGaugeBuilder longGauge(String name, ToDoubleFunction<LongAdder> func) {
    return longGauges.builder(baseName() + name, func);
  }

  DistributionSummary.Builder distributionSummary(String name) {
    return DistributionSummary.builder(baseName() + name);
  }

  Timer.Builder timer(String name) {
    return Timer.builder(baseName() + name);
  }
}
