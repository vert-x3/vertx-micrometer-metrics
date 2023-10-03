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

  <T1> Tags toTags(Label l1, Function<T1, String> func1, T1 v1) {
    return enabledLabels.contains(l1) ? Tags.of(l1.toString(), func1.apply(v1)) : Tags.empty();
  }

  <T1, T11> Tags toTags(Label l1, BiFunction<T1, T11, String> func1, T1 v1, T11 v11) {
    return enabledLabels.contains(l1) ? Tags.of(l1.toString(), func1.apply(v1, v11)) : Tags.empty();
  }

  <T1, T2> Tags toTags(Label l1, Function<T1, String> func1, T1 v1, Label l2, Function<T2, String> func2, T2 v2) {
    if (enabledLabels.contains(l1)) {
      if (enabledLabels.contains(l2)) {
        return Tags.of(
          Tag.of(l1.toString(), func1.apply(v1)),
          Tag.of(l2.toString(), func2.apply(v2))
        );
      }
      return Tags.of(l1.toString(), func1.apply(v1));
    }
    return toTags(l2, func2, v2);
  }
}
