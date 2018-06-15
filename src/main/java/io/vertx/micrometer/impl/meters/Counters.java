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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.Labels;

/**
 * @author Joel Takvorian
 */
public class Counters {
  private final String name;
  private final String description;
  private final Label[] keys;
  private final MeterRegistry registry;

  public Counters(String name,
                  String description,
                  MeterRegistry registry,
                  Label... keys) {
    this.name = name;
    this.description = description;
    this.registry = registry;
    this.keys = keys;
  }

  public Counter get(String... values) {
    // Get or create the Counter
    return Counter.builder(name)
      .description(description)
      .tags(Labels.toTags(keys, values))
      .register(registry);
  }
}
