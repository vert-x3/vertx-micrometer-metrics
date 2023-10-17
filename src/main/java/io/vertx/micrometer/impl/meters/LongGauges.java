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

import io.micrometer.core.instrument.Meter;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;

public class LongGauges {

  private final ConcurrentMap<Meter.Id, LongAdder> longGauges;

  public LongGauges(ConcurrentMap<Meter.Id, LongAdder> longGauges) {
    this.longGauges = longGauges;
  }

  public LongGaugeBuilder builder(String name, ToDoubleFunction<LongAdder> func) {
    return new LongGaugeBuilder(name, longGauges, func);
  }
}
