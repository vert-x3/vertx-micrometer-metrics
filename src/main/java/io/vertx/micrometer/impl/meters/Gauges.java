/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import io.micrometer.core.instrument.Tags;
import io.vertx.micrometer.Label;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * @author Joel Takvorian
 */
public class Gauges<T> {
  private final String name;
  private final String description;
  private final Label[] keys;
  private final Function<Meter.Id, T> tSupplier;
  private final ToDoubleFunction<T> dGetter;
  private final MeterRegistry registry;
  private final ConcurrentMap<Meter.Id, Object> gauges;

  public Gauges(ConcurrentMap<Meter.Id, Object> gauges,
                String name,
                String description,
                Supplier<T> tSupplier,
                ToDoubleFunction<T> dGetter,
                MeterRegistry registry,
                Label... keys) {
    this.gauges = gauges;
    this.name = name;
    this.description = description;
    this.tSupplier = id -> tSupplier.get();
    this.dGetter = dGetter;
    this.registry = registry;
    this.keys = keys;
  }

  public T get(String... values) {
    return get(null, values);
  }

  @SuppressWarnings("unchecked")
  public T get(Iterable<Tag> customTags, String... values) {
    Tags tags = TagsCache.getOrCreate(customTags, keys, values);
    ValueSupplier<T> supplier = new ValueSupplier<>(gauges, dGetter);
    Gauge gauge = Gauge.builder(name, supplier)
      .description(description)
      .tags(tags)
      .strongReference(true)
      .register(registry);
    Meter.Id meterId = gauge.getId();
    supplier.id = meterId;
    return (T) gauges.computeIfAbsent(meterId, tSupplier);
  }

  private static class ValueSupplier<G> implements Supplier<Number> {
    final ConcurrentMap<Meter.Id, Object> gauges;
    final ToDoubleFunction<G> toDoubleFunc;
    volatile Meter.Id id;

    ValueSupplier(ConcurrentMap<Meter.Id, Object> gauges, ToDoubleFunction<G> toDoubleFunc) {
      this.gauges = gauges;
      this.toDoubleFunc = toDoubleFunc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Number get() {
      Meter.Id key = id;
      if (key != null) {
        Object o = gauges.get(key);
        if (o != null) {
          return toDoubleFunc.applyAsDouble((G) o);
        }
      }
      return 0.0D;
    }
  }
}
