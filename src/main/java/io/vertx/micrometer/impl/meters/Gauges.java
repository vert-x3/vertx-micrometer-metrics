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
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.noop.NoopGauge;
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
    this.tSupplier = tSupplier;
    this.dGetter = dGetter;
    this.registry = registry;
    this.keys = keys;
  }

  @SuppressWarnings("unchecked")
  public T get(String... values) {
    Tags tags = Tags.of(Labels.toTags(keys, values));
    T candidate = tSupplier.get();
    ToDoubleFunc<T> candidateFunc = new ToDoubleFunc<>(dGetter);
    Gauge gauge = Gauge.builder(name, candidate, candidateFunc)
      .description(description)
      .tags(Labels.toTags(keys, values))
      .register(registry);
    Meter.Id gaugeId = gauge.getId();
    Object res;
    for (; ; ) {
      res = gauges.get(gaugeId);
      if (res != null) {
        break;
      }
      if (gauge instanceof NoopGauge) {
        res = candidate;
        break;
      }
      ensureGetterInvoked(gauge);
      if (candidateFunc.invoked) {
        gauges.put(gaugeId, candidate);
        res = candidate;
        break;
      }
    }
    return (T) res;
  }

  private void ensureGetterInvoked(Gauge gauge) {
    gauge.value();
  }

  private static class ToDoubleFunc<R> implements ToDoubleFunction<R> {
    final ToDoubleFunction<R> delegate;
    volatile boolean invoked;

    ToDoubleFunc(ToDoubleFunction<R> delegate) {
      this.delegate = delegate;
    }

    @Override
    public double applyAsDouble(R value) {
      if (!invoked) {
        invoked = true;
      }
      return delegate.applyAsDouble(value);
    }
  }
}
