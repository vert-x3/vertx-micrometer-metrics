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

package io.vertx.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Counters;
import io.vertx.monitoring.meters.Gauges;
import io.vertx.monitoring.meters.Summaries;
import io.vertx.monitoring.meters.Timers;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Abstract class for metrics container.
 *
 * @author Joel Takvorian
 */
public abstract class AbstractMetrics implements MicrometerMetrics {
  protected final LabelMatchers labelMatchers;
  protected final MeterRegistry registry;
  protected final MetricsCategory domain;
  protected final String baseName;

  AbstractMetrics(LabelMatchers labelMatchers, MeterRegistry registry, MetricsCategory domain, String baseName) {
    this.labelMatchers = labelMatchers;
    this.registry = registry;
    this.domain = domain;
    this.baseName = baseName;
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
    return baseName;
  }

  Counters counters(String name, String description, String... keys) {
    return new Counters(domain, baseName + name, description, registry, keys);
  }

  Gauges<LongAdder> longGauges(String name, String description, String... keys) {
    return new Gauges<>(domain, baseName + name, description, LongAdder::new, LongAdder::doubleValue, registry, keys);
  }

  Gauges<AtomicReference<Double>> doubleGauges(String name, String description, String... keys) {
    return new Gauges<>(domain, baseName + name, description, () -> new AtomicReference<>(0d), AtomicReference::get, registry, keys);
  }

  Summaries summaries(String name, String description, String... keys) {
    return new Summaries(domain, baseName + name, description, registry, keys);
  }

  Timers timers(String name, String description, String... keys) {
    return new Timers(domain, baseName + name, description, registry, keys);
  }
}
