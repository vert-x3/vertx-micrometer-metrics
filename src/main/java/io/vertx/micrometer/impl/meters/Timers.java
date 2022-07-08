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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.Labels;

import java.util.concurrent.TimeUnit;

/**
 * @author Joel Takvorian
 */
public class Timers {
  private final String name;
  private final String description;
  private final Label[] keys;
  private final MeterRegistry registry;

  public Timers(String name,
                String description,
                MeterRegistry registry,
                Label... keys) {
    this.name = name;
    this.description = description;
    this.registry = registry;
    this.keys = keys;
  }

  public Timer get(String... values) {
    return get(null, values);
  }

  public Timer get(Iterable<Tag> customTags, String... values) {
    Tags tags = Tags.of(Labels.toTags(keys, values)).and(customTags);
    return Timer.builder(name)
      .description(description)
      .tags(tags)
      .register(registry);
  }

  public EventTiming start() {
    return new EventTiming(this);
  }

  public static class EventTiming {
    private final Timers ref;
    private final long nanoStart;

    private EventTiming(Timers ref) {
      this.ref = ref;
      this.nanoStart = System.nanoTime();
    }

    public void end(String... values) {
      end(null, values);
    }

    public void end(Iterable<Tag> customTags, String... values) {
      Timer t = ref.get(customTags, values);
      t.record(System.nanoTime() - nanoStart, TimeUnit.NANOSECONDS);
    }
  }
}
