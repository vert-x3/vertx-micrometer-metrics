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

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;

import static io.micrometer.core.instrument.Meter.Type.*;

class MeterCache {

  private static final Object COUNTERS = new Object();
  private static final Object LONG_GAUGES = new Object();
  private static final Object DISTRIBUTION_SUMMARIES = new Object();
  private static final Object TIMERS = new Object();

  private final boolean enabled;
  private final MeterRegistry registry;
  private final LongGauges longGauges;

  MeterCache(boolean enabled, MeterRegistry registry, LongGauges longGauges) {
    this.enabled = enabled;
    this.registry = registry;
    this.longGauges = longGauges;
  }

  Counter getOrCreateCounter(String name, String description, Tags tags) {
    Counter counter;
    ContextInternal context;
    if (enabled && (context = eventLoopContext()) != null) {
      @SuppressWarnings("unchecked")
      Map<Meter.Id, Counter> counterMap = (Map<Meter.Id, Counter>) context.contextData().computeIfAbsent(COUNTERS, k -> new HashMap<>());
      Meter.Id id = new Meter.Id(name, tags, null, description, COUNTER);
      counter = counterMap.get(id);
      if (counter == null) {
        counter = Counter.builder(name).description(description).tags(tags).register(registry);
        counterMap.put(id, counter);
      }
    } else {
      counter = Counter.builder(name).description(description).tags(tags).register(registry);
    }
    return counter;
  }

  LongAdder getOrCreateLongGauge(String name, String description, Tags tags, ToDoubleFunction<LongAdder> func) {
    LongAdder longGauge;
    ContextInternal context;
    if (enabled && (context = eventLoopContext()) != null) {
      @SuppressWarnings("unchecked")
      Map<Meter.Id, LongAdder> longGaugeMap = (Map<Meter.Id, LongAdder>) context.contextData().computeIfAbsent(LONG_GAUGES, k -> new HashMap<>());
      Meter.Id id = new Meter.Id(name, tags, null, description, GAUGE);
      longGauge = longGaugeMap.get(id);
      if (longGauge == null) {
        longGauge = longGauges.builder(name, func).description(description).tags(tags).register(registry);
        longGaugeMap.put(id, longGauge);
      }
    } else {
      longGauge = longGauges.builder(name, func).description(description).tags(tags).register(registry);
    }
    return longGauge;
  }

  DistributionSummary getOrCreateDistributionSummary(String name, String description, Tags tags) {
    DistributionSummary distributionSummary;
    ContextInternal context;
    if (enabled && (context = eventLoopContext()) != null) {
      @SuppressWarnings("unchecked")
      Map<Meter.Id, DistributionSummary> distributionSummaryMap = (Map<Meter.Id, DistributionSummary>) context.contextData().computeIfAbsent(DISTRIBUTION_SUMMARIES, k -> new HashMap<>());
      Meter.Id id = new Meter.Id(name, tags, null, description, DISTRIBUTION_SUMMARY);
      distributionSummary = distributionSummaryMap.get(id);
      if (distributionSummary == null) {
        distributionSummary = DistributionSummary.builder(name).description(description).tags(tags).register(registry);
        distributionSummaryMap.put(id, distributionSummary);
      }
    } else {
      distributionSummary = DistributionSummary.builder(name).description(description).tags(tags).register(registry);
    }
    return distributionSummary;
  }

  Timer getOrCreateTimer(String name, String description, Tags tags) {
    Timer timer;
    ContextInternal context;
    if (enabled && (context = eventLoopContext()) != null) {
      @SuppressWarnings("unchecked")
      Map<Meter.Id, Timer> timerMap = (Map<Meter.Id, Timer>) context.contextData().computeIfAbsent(TIMERS, k -> new HashMap<>());
      Meter.Id id = new Meter.Id(name, tags, null, description, TIMER);
      timer = timerMap.get(id);
      if (timer == null) {
        timer = Timer.builder(name).description(description).tags(tags).register(registry);
        timerMap.put(id, timer);
      }
    } else {
      timer = Timer.builder(name).description(description).tags(tags).register(registry);
    }
    return timer;
  }

  private ContextInternal eventLoopContext() {
    // Caching is only safe when running on an event-loop context since we store meters in a HashMap
    ContextInternal current = ContextInternal.current();
    return current != null && current.isEventLoopContext() && current.inThread() ? current : null;
  }
}
