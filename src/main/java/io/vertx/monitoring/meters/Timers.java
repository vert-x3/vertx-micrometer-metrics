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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.noop.NoopTimer;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.Labels;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Joel Takvorian
 */
public class Timers {
  private static final Timer NOOP_TIMER = new NoopTimer(null);

  private final MetricsCategory domain;
  private final String name;
  private final String description;
  private final String[] keys;
  private final MeterRegistry registry;
  private final Map<Labels.Values, Timer> timers = new ConcurrentHashMap<>();

  public Timers(MetricsCategory domain,
                String name,
                String description,
                MeterRegistry registry,
                String... keys) {
    this.domain = domain;
    this.name = name;
    this.description = description;
    this.registry = registry;
    this.keys = keys;
  }

  public Timer get(LabelMatchers labelMatchers, String... values) {
    return timers.computeIfAbsent(new Labels.Values(values), v -> {
      // Match labels. If match fails, do not store a new Timer
      List<Tag> tags = labelMatchers.toTags(domain, keys, values);
      if (tags == null) {
        return NOOP_TIMER;
      }
      // Create a new Timer
      return Timer.builder(name)
        .description(description)
        .tags(tags)
        .register(registry);
    });
  }

  public EventTiming start(LabelMatchers labelMatchers, String... values) {
    Timer t = get(labelMatchers, values);
    return new EventTiming(t);
  }

  public static class EventTiming {
    private final Timer timer;
    private final long nanoStart;

    private EventTiming(Timer timer) {
      this.timer = timer;
      this.nanoStart = System.nanoTime();
    }

    public void end() {
      timer.record(System.nanoTime() - nanoStart, TimeUnit.NANOSECONDS);
    }
  }
}
