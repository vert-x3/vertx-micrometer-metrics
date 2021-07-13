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
package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.Labels;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Joel Takvorian
 */
public class Gauges<T> {
  private final String name;
  private final String description;
  private final Label[] keys;
  private final Supplier<T> tSupplier;
  private final ToDoubleFunction<T> dGetter;
  private final MeterRegistry registry;
  private final Map<Meter.Id, T> gauges = new ConcurrentHashMap<>();

  public Gauges(String name,
                String description,
                Supplier<T> tSupplier,
                ToDoubleFunction<T> dGetter,
                MeterRegistry registry,
                Label... keys) {
    this.name = name;
    this.description = description;
    this.tSupplier = tSupplier;
    this.dGetter = dGetter;
    this.registry = registry;
    this.keys = keys;
  }

  public T get(String... values) {
    return get(null, values);
  }

  public synchronized T get(Iterable<Tag> customTags, String... values) {
    List<Tag> tags = customTags != null
      ? Stream.concat(Labels.toTags(keys, values).stream(), StreamSupport.stream(customTags.spliterator(), false)).collect(Collectors.toList())
      : Labels.toTags(keys, values);
    // This method is synchronized to make sure the "T" built via supplier will match the one passed to Gauge
    //  since it is stored as WeakReference in Micrometer DefaultGauge, it must not be lost.
    T t = tSupplier.get();
    // Register this gauge if necessary
    // Note: we need here to go through the process of Gauge creation, even if it already exists,
    //  in order to get the Gauge ID. This ID generation is not trivial since it may involves attached MetricFilters.
    //  Micrometer will not register the gauge twice if it was already created.
    Gauge g = Gauge.builder(name, t, dGetter)
      .description(description)
      .tags(tags)
      .register(registry);
    return gauges.computeIfAbsent(g.getId(), v -> t);
  }
}
