/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class LongGaugeBuilder {

  private final LongAdderSupplier supplier;
  private final Gauge.Builder<Supplier<Number>> builder;
  private final ConcurrentMap<Meter.Id, LongAdder> longGauges;

  LongGaugeBuilder(String name, ConcurrentMap<Meter.Id, LongAdder> longGauges, ToDoubleFunction<LongAdder> func) {
    this.supplier = new LongAdderSupplier(longGauges, func);
    this.builder = Gauge.builder(name, supplier);
    this.longGauges = longGauges;
  }

  public LongGaugeBuilder description(String description) {
    builder.description(description);
    return this;
  }

  public LongGaugeBuilder tags(Iterable<Tag> tags) {
    builder.tags(tags);
    return this;
  }

  public LongAdder register(MeterRegistry registry) {
    Meter.Id meterId = builder.register(registry).getId();
    LongAdder res = longGauges.get(meterId);
    if (res == null) {
      LongAdder candidate = new LongAdder();
      if ((res = longGauges.putIfAbsent(meterId, candidate)) == null) {
        res = candidate;
        supplier.setId(meterId);
        return res;
      }
    }
    return res;
  }
}
