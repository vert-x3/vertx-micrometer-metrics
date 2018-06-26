/*
 * Copyright 2014 Red Hat, Inc.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Abstract class for metrics container.
 *
 * @author Joel Takvorian
 */
public abstract class AbstractMetrics implements MicrometerMetrics {
  protected final MeterRegistry registry;
  protected final MetricsDomain domain;

  AbstractMetrics(MeterRegistry registry, MetricsDomain domain) {
    this.registry = registry;
    this.domain = domain;
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
    return domain == null ? null : domain.getPrefix();
  }

  Counters counters(String name, String description, Label... keys) {
    return new Counters(domain.getPrefix() + name, description, registry, keys);
  }

  Gauges<LongAdder> longGauges(String name, String description, Label... keys) {
    return new Gauges<>(domain.getPrefix() + name, description, LongAdder::new, LongAdder::doubleValue, registry, keys);
  }

  Gauges<AtomicReference<Double>> doubleGauges(String name, String description, Label... keys) {
    return new Gauges<>(domain.getPrefix() + name, description, () -> new AtomicReference<>(0d), AtomicReference::get, registry, keys);
  }

  Summaries summaries(String name, String description, Label... keys) {
    return new Summaries(domain.getPrefix() + name, description, registry, keys);
  }

  Timers timers(String name, String description, Label... keys) {
    return new Timers(domain.getPrefix() + name, description, registry, keys);
  }
}
