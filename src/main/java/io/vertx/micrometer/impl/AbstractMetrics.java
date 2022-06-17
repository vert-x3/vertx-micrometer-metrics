/*
 * Copyright 2022 Red Hat, Inc.
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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Abstract class for metrics container.
 *
 * @author Joel Takvorian
 */
public abstract class AbstractMetrics implements MicrometerMetrics {
  protected final MeterRegistry registry;
  protected final String category;
  protected final ConcurrentMap<Meter.Id, Object> gaugesTable;

  AbstractMetrics(MeterRegistry registry, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    this.registry = registry;
    this.gaugesTable = gaugesTable;
    this.category = null;
  }

  AbstractMetrics(MeterRegistry registry, String category, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    this.registry = registry;
    this.category = category;
    this.gaugesTable = gaugesTable;
  }

  AbstractMetrics(MeterRegistry registry, MetricsDomain domain, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    this.registry = registry;
    this.category = (domain == null) ? null : domain.toCategory();
    this.gaugesTable = gaugesTable;
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

  Counters counters(String name, String description, Label... keys) {
    return new Counters(baseName() + name, description, registry, keys);
  }

  Gauges<LongAdder> longGauges(String name, String description, Label... keys) {
    return new Gauges<>(gaugesTable, baseName() + name, description, LongAdder::new, LongAdder::doubleValue, registry, keys);
  }

  Gauges<AtomicReference<Double>> doubleGauges(String name, String description, Label... keys) {
    return new Gauges<>(gaugesTable, baseName() + name, description, () -> new AtomicReference<>(0d), AtomicReference::get, registry, keys);
  }

  Summaries summaries(String name, String description, Label... keys) {
    return new Summaries(baseName() + name, description, registry, keys);
  }

  Timers timers(String name, String description, Label... keys) {
    return new Timers(baseName() + name, description, registry, keys);
  }
}
