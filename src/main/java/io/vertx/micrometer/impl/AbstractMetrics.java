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

import io.micrometer.core.instrument.*;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;
import io.vertx.micrometer.impl.tags.IgnoredTag;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Abstract class for metrics container.
 *
 * @author Joel Takvorian
 */
public abstract class AbstractMetrics implements MicrometerMetrics {

  protected final MeterRegistry registry;
  protected final MetricsNaming names;
  private final String category;
  private final EnumSet<Label> enabledLabels;
  private final MeterCache meterCache;

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels, boolean meterCacheEnabled) {
    this.registry = registry;
    this.category = null;
    this.enabledLabels = enabledLabels;
    this.names = names;
    this.meterCache = new MeterCache(meterCacheEnabled, registry, longGauges);
  }

  AbstractMetrics(AbstractMetrics parent, MetricsDomain domain) {
    this(parent, domain == null ? null : domain.toCategory());
  }

  AbstractMetrics(AbstractMetrics parent, String category) {
    this.registry = parent.registry;
    this.enabledLabels = parent.enabledLabels;
    this.meterCache = parent.meterCache;
    this.category = category;
    this.names = parent.names.withBaseName(baseName());
  }

  /**
   * @return the Micrometer registry used with this measured object.
   */
  @Override
  public MeterRegistry registry() {
    return registry;
  }

  // Method is final because it is invoked from the constructor
  @Override
  public final String baseName() {
    return category == null ? null : "vertx." + category + ".";
  }

  Counter counter(String name, String description, Tags tags) {
    return meterCache.getOrCreateCounter(name, description, tags);
  }

  LongAdder longGauge(String name, String description, Tags tags) {
    return longGauge(name, description, tags, LongAdder::doubleValue);
  }

  LongAdder longGauge(String name, String description, Tags tags, ToDoubleFunction<LongAdder> func) {
    return meterCache.getOrCreateLongGauge(name, description, tags, func);
  }

  DistributionSummary distributionSummary(String name, String description, Tags tags) {
    return meterCache.getOrCreateDistributionSummary(name, description, tags);
  }

  Timer timer(String name, String description, Tags tags) {
    return meterCache.getOrCreateTimer(name, description, tags);
  }

  <U> Tag toTag(Label label, Function<U, String> func, U u) {
    return enabledLabels.contains(label) ? Tag.of(label.toString(), func.apply(u)) : IgnoredTag.INSTANCE;
  }

  <U, V> Tag toTag(Label label, BiFunction<U, V, String> func, U u, V v) {
    return enabledLabels.contains(label) ? Tag.of(label.toString(), func.apply(u, v)) : IgnoredTag.INSTANCE;
  }
}
