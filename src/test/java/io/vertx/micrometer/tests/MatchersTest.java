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

package io.vertx.micrometer.tests;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static io.vertx.micrometer.Label.EB_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class MatchersTest {

  @Test
  public void shouldFilterMetric() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.EQUALS)
      .setValue("addr1")));
    Counter c1 = Counter.builder("my_counter").tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    c1.increment();
    Counter c2 = Counter.builder("my_counter").tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    c2.increment();

    Counter c = registry.find("my_counter").tags("address", "addr1").counter();
    assertThat(c).isNotNull().extracting(Counter::count).isEqualTo(1d);
    c = registry.find("my_counter").tags("address", "addr2").counter();
    assertThat(c).isNull();
  }

  @Test
  public void shouldFilterDomainMetric() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setDomain(MetricsDomain.EVENT_BUS)
      .setType(MatchType.EQUALS)
      .setValue("addr1")));
    String metric1 = MetricsDomain.EVENT_BUS.getPrefix() + "_counter";
    Counter c1 = Counter.builder(metric1).tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    c1.increment();
    Counter c2 = Counter.builder(metric1).tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    c2.increment();
    String metric2 = "another_domain_counter";
    Counter c3 = Counter.builder(metric2).tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    c3.increment();
    Counter c4 = Counter.builder(metric2).tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    c4.increment();

    // In domain where the rule applies, filter is performed
    Counter c = registry.find(metric1).tags("address", "addr1").counter();
    assertThat(c).isNotNull().extracting(Counter::count).isEqualTo(1d);
    c = registry.find(metric1).tags("address", "addr2").counter();
    assertThat(c).isNull();
    // In other domain, no filter
    c = registry.find(metric2).tags("address", "addr1").counter();
    assertThat(c).isNotNull().extracting(Counter::count).isEqualTo(1d);
    c = registry.find(metric2).tags("address", "addr2").counter();
    assertThat(c).isNotNull().extracting(Counter::count).isEqualTo(1d);
  }
}
