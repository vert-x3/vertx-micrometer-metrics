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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.noop.NoopCounter;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.Labels;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Joel Takvorian
 */
public class Counters {
  private static final Counter NOOP_COUNTER = new NoopCounter(null);

  private final MetricsCategory domain;
  private final String name;
  private final String description;
  private final String[] keys;
  private final MeterRegistry registry;
  private final Map<Labels.Values, Counter> counters = new ConcurrentHashMap<>();

  public Counters(MetricsCategory domain,
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

  public Counter get(LabelMatchers labelMatchers, String... values) {
    return counters.computeIfAbsent(new Labels.Values(values), v -> {
      // Match labels. If match fails, do not store a new counter
      List<Tag> tags = labelMatchers.toTags(domain, keys, values);
      if (tags == null) {
        return NOOP_COUNTER;
      }
      // Create a new Counter
      return Counter.builder(name)
        .description(description)
        .tags(tags)
        .register(registry);
    });
  }
}
