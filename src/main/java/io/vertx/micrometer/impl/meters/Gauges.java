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

import io.micrometer.core.instrument.*;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.Labels;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

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
  private final ConcurrentMap<Meter.Id, Object> candidatesByMeterId;

  public Gauges(ConcurrentMap<Meter.Id, Object> gauges,
                String name,
                String description,
                Supplier<T> tSupplier,
                ToDoubleFunction<T> dGetter,
                MeterRegistry registry,
                Label... keys) {
    this.candidatesByMeterId = gauges;
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

  @SuppressWarnings("unchecked")
  public T get(Iterable<Tag> customTags, String... values) {
    T candidate = tSupplier.get();

    Gauge gauge = Gauge.builder(name, candidate, dGetter)
      .description(description)
      .tags(Tags.of(Labels.toTags(keys, values)).and(customTags))
      .register(registry);
    Meter.Id gaugeId = gauge.getId();

    return (T) candidatesByMeterId.computeIfAbsent(gaugeId, id -> candidate);
  }
}
