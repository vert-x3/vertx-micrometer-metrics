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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.Labels;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.EB_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class GaugesTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  private LongGauges longGauges = new LongGauges(new ConcurrentHashMap<>());

  @Test
  public void shouldAliasGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    LongAdder g1 = longGauges.builder("my_gauge", LongAdder::doubleValue).tags(Labels.toTags(EB_ADDRESS, "addr1")).register(registry);
    g1.increment();
    g1.increment();
    LongAdder g2 = longGauges.builder("my_gauge", LongAdder::doubleValue).tags(Labels.toTags(EB_ADDRESS, "addr2")).register(registry);
    g2.increment();

    Gauge g = registry.get("my_gauge").tags("address", "1").gauge();
    assertThat(g.value()).isEqualTo(2d);
    g = registry.find("my_gauge").tags("address", "addr1").gauge();
    assertThat(g).isNull();
    g = registry.get("my_gauge").tags("address", "addr2").gauge();
    assertThat(g.value()).isEqualTo(1d);
  }

  @Test
  public void shouldIgnoreGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    LongAdder g1 = longGauges.builder("my_gauge", LongAdder::doubleValue).tags(Labels.toTags(EB_ADDRESS, "addr1")).register(registry);
    g1.increment();
    g1.increment();
    LongAdder g2 = longGauges.builder("my_gauge", LongAdder::doubleValue).tags(Labels.toTags(EB_ADDRESS, "addr2")).register(registry);
    g2.increment();

    Gauge g = registry.get("my_gauge").tags("address", "_").gauge();
    assertThat(g.value()).isEqualTo(3d);
    g = registry.find("my_gauge").tags("address", "addr1").gauge();
    assertThat(g).isNull();
    g = registry.find("my_gauge").tags("address", "addr2").gauge();
    assertThat(g).isNull();
  }

  @Test
  public void shouldSupportNoopGauges() {
    MeterRegistry registry = new SimpleMeterRegistry();
    registry.config().meterFilter(MeterFilter.deny(id -> "my_gauge".equals(id.getName())));
    LongAdder g1 = longGauges.builder("my_gauge", LongAdder::doubleValue).register(registry);
    g1.increment();

    assertThat(registry.find("my_gauge").gauges()).isEmpty();
  }
}
