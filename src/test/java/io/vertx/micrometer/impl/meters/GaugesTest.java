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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Joel Takvorian
 */
public class GaugesTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  ConcurrentMap<Meter.Id, Object> gaugesTable = new ConcurrentHashMap<>();

  @Test
  public void shouldAliasGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Gauges<LongAdder> gauges = new Gauges<>(gaugesTable, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get("addr1").increment();
    gauges.get("addr1").increment();
    gauges.get("addr2").increment();

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
    Gauges<LongAdder> gauges = new Gauges<>(gaugesTable, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get("addr1").increment();
    gauges.get("addr1").increment();
    gauges.get("addr2").increment();

    Gauge g = registry.get("my_gauge").tags("address", "_").gauge();
    assertThat(g.value()).isEqualTo(3d);
    g = registry.find("my_gauge").tags("address", "addr1").gauge();
    assertThat(g).isNull();
    g = registry.find("my_gauge").tags("address", "addr2").gauge();
    assertThat(g).isNull();
  }

  @Test
  public void shouldAddCustomTags() {
    List<Tag> customTags = Arrays.asList(Tag.of("k1", "v1"), Tag.of("k2", "v2"));
    MeterRegistry registry = new SimpleMeterRegistry();
    Gauges<LongAdder> gauges = new Gauges<>(gaugesTable, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get(customTags, "addr1").increment();

    Gauge g = registry.get("my_gauge").tags("address", "addr1", "k1", "v1", "k2", "v2").gauge();
    assertThat(g.value()).isEqualTo(1d);
  }

  @Test
  public void shouldAllowFilteringGauges() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Gauges<LongAdder> gauges = new Gauges<>(gaugesTable, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get("counterObject").increment();
    gauges.get("filteredCounterObject").increment();

    assertThat(registry.get("my_gauge").tags("address", "counterObject").gauge().value()).isEqualTo(1d);
    assertThat(registry.get("my_gauge").tags("address", "filteredCounterObject").gauge().value()).isEqualTo(1d);

    registry.config().meterFilter(MeterFilter.deny(id -> "my_gauge".equals(id.getName()) && "filteredCounterObject".equals(id.getTag("address"))));
    registry.remove(registry.get("my_gauge").tags("address", "filteredCounterObject").gauge());

    gauges.get("counterObject").increment();
    gauges.get("filteredCounterObject").increment();

    assertThat(registry.get("my_gauge").tags("address", "counterObject").gauge().value()).isEqualTo(2d);
    assertThatThrownBy(() -> registry.get("my_gauge").tags("address", "filteredCounterObject").gauge()).isInstanceOf(io.micrometer.core.instrument.search.MeterNotFoundException.class);
  }
}
