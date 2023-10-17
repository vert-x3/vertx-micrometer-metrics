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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;

import java.util.Collections;

import static io.vertx.micrometer.Label.EB_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class SummariesTest {

  @Test
  public void shouldAliasSummaryLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    DistributionSummary s1 = DistributionSummary.builder("my_summary").tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    s1.record(5);
    s1.record(8);
    DistributionSummary s2 = DistributionSummary.builder("my_summary").tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    s2.record(10);

    DistributionSummary s = registry.find("my_summary").tags("address", "1").summary();
    assertThat(s).isNotNull().extracting(DistributionSummary::count).containsExactly(2L);
    assertThat(s.totalAmount()).isEqualTo(13);
    s = registry.find("my_summary").tags("address", "addr1").summary();
    assertThat(s).isNull();
    s = registry.find("my_summary").tags("address", "addr2").summary();
    assertThat(s).isNotNull().extracting(DistributionSummary::count).containsExactly(1L);
    assertThat(s.totalAmount()).isEqualTo(10);
  }

  @Test
  public void shouldIgnoreSummaryLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    DistributionSummary s1 = DistributionSummary.builder("my_summary").tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    s1.record(5);
    s1.record(8);
    DistributionSummary s2 = DistributionSummary.builder("my_summary").tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    s2.record(10);

    DistributionSummary s = registry.find("my_summary").tags("address", "_").summary();
    assertThat(s).isNotNull().extracting(DistributionSummary::count).containsExactly(3L);
    assertThat(s.totalAmount()).isEqualTo(23);
    s = registry.find("my_summary").tags("address", "addr1").summary();
    assertThat(s).isNull();
    s = registry.find("my_summary").tags("address", "addr2").summary();
    assertThat(s).isNull();
  }
}
