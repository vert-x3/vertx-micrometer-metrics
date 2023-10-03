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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.Labels;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;

import static io.vertx.micrometer.Label.EB_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class CountersTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  @Test
  public void shouldAliasCounterLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Counter c1 = Counter.builder("my_counter").tags(Labels.toTags(EB_ADDRESS, "addr1")).register(registry);
    c1.increment();
    c1.increment();
    Counter c2 = Counter.builder("my_counter").tags(Labels.toTags(EB_ADDRESS, "addr2")).register(registry);
    c2.increment();

    Counter c = registry.find("my_counter").tags("address", "1").counter();
    assertThat(c).isNotNull().extracting(Counter::count).containsExactly(2d);
    c = registry.find("my_counter").tags("address", "addr1").counter();
    assertThat(c).isNull();
    c = registry.find("my_counter").tags("address", "addr2").counter();
    assertThat(c).isNotNull().extracting(Counter::count).containsExactly(1d);
  }

  @Test
  public void shouldIgnoreCounterLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Counter c1 = Counter.builder("my_counter").tags(Labels.toTags(EB_ADDRESS, "addr1")).register(registry);
    c1.increment();
    c1.increment();
    Counter c2 = Counter.builder("my_counter").tags(Labels.toTags(EB_ADDRESS, "addr2")).register(registry);
    c2.increment();

    Counter c = registry.find("my_counter").tags("address", "_").counter();
    assertThat(c).isNotNull().extracting(Counter::count).containsExactly(3d);
    c = registry.find("my_counter").tags("address", "addr1").counter();
    assertThat(c).isNull();
    c = registry.find("my_counter").tags("address", "addr2").counter();
    assertThat(c).isNull();
  }
}
