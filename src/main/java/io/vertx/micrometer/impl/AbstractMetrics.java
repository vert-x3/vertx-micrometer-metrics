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
import io.vertx.micrometer.impl.meters.LongGaugeBuilder;
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
  protected final String category;
  protected final LongGauges longGauges;
  protected final EnumSet<Label> enabledLabels;

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    this.registry = registry;
    this.longGauges = longGauges;
    this.category = null;
    this.enabledLabels = enabledLabels;
    this.names = names.withBaseName(baseName());
  }

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, String category, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    this.registry = registry;
    this.category = category;
    this.longGauges = longGauges;
    this.enabledLabels = enabledLabels;
    this.names = names.withBaseName(baseName());
  }

  AbstractMetrics(MeterRegistry registry, MetricsNaming names, MetricsDomain domain, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    this.registry = registry;
    this.category = (domain == null) ? null : domain.toCategory();
    this.longGauges = longGauges;
    this.enabledLabels = enabledLabels;
    this.names = names.withBaseName(baseName());
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
    return Counter.builder(name);
  }

  LongGaugeBuilder longGauge(String name) {
    return longGauges.builder(name, LongAdder::doubleValue);
  }

  LongGaugeBuilder longGauge(String name, ToDoubleFunction<LongAdder> func) {
    return longGauges.builder(name, func);
  }

  DistributionSummary.Builder distributionSummary(String name) {
    return DistributionSummary.builder(name);
  }

  Timer.Builder timer(String name) {
    return Timer.builder(name);
  }

  <U> Tag toTag(Label label, Function<U, String> func, U u) {
    return enabledLabels.contains(label) ? Tag.of(label.toString(), func.apply(u)) : IgnoredTag.INSTANCE;
  }

  <U, V> Tag toTag(Label label, BiFunction<U, V, String> func, U u, V v) {
    return enabledLabels.contains(label) ? Tag.of(label.toString(), func.apply(u, v)) : IgnoredTag.INSTANCE;
  }
}
