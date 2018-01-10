/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.vertx.monitoring.Labels;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.match.LabelMatchers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * @author Joel Takvorian
 */
public class Gauges<T> {
  private final MetricsCategory domain;
  private final String name;
  private final String description;
  private final String[] keys;
  private final Supplier<T> tSupplier;
  private final ToDoubleFunction<T> dGetter;
  private final MeterRegistry registry;
  private final Map<Labels.Values, T> gauges = new ConcurrentHashMap<>();

  public Gauges(MetricsCategory domain,
                String name,
                String description,
                Supplier<T> tSupplier,
                ToDoubleFunction<T> dGetter,
                MeterRegistry registry,
                String... keys) {
    this.domain = domain;
    this.name = name;
    this.description = description;
    this.tSupplier = tSupplier;
    this.dGetter = dGetter;
    this.registry = registry;
    this.keys = keys;
  }

  public T get(LabelMatchers labelMatchers, String... values) {
    return gauges.computeIfAbsent(new Labels.Values(values), v -> {
      // Create a new Gauge for this handler
      T t = tSupplier.get();
      // Match labels. If match fails, do not store a new gauge
      List<Tag> tags = labelMatchers.toTags(domain, keys, values);
      if (tags != null) {
        Gauge.builder(name, t, dGetter)
          .description(description)
          .tags(tags)
          .register(registry);
      }
      return t;
    });
  }
}
