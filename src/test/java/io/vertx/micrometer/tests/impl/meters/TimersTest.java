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

package io.vertx.micrometer.tests.impl.meters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static io.vertx.micrometer.Label.EB_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class TimersTest {

  @Test
  public void shouldAliasTimerLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Timer t1 = Timer.builder("my_timer").tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    t1.record(5, TimeUnit.MILLISECONDS);
    t1.record(8, TimeUnit.MILLISECONDS);
    Timer t2 = Timer.builder("my_timer").tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    t2.record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.find("my_timer").tags("address", "1").timer();
    assertThat(t).isNotNull().extracting(Timer::count).isEqualTo(2L);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(13);
    t = registry.find("my_timer").tags("address", "addr1").timer();
    assertThat(t).isNull();
    t = registry.find("my_timer").tags("address", "addr2").timer();
    assertThat(t).isNotNull().extracting(Timer::count).isEqualTo(1L);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(10);
  }

  @Test
  public void shouldIgnoreTimerLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Timer t1 = Timer.builder("my_timer").tags(Tags.of(EB_ADDRESS.toString(), "addr1")).register(registry);
    t1.record(5, TimeUnit.MILLISECONDS);
    t1.record(8, TimeUnit.MILLISECONDS);
    Timer t2 = Timer.builder("my_timer").tags(Tags.of(EB_ADDRESS.toString(), "addr2")).register(registry);
    t2.record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.find("my_timer").timer();
    assertThat(t).isNotNull().extracting(Timer::count).isEqualTo(3L);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(23);
    t = registry.find("my_timer").tags("address", "addr1").timer();
    assertThat(t).isNull();
    t = registry.find("my_timer").tags("address", "addr2").timer();
    assertThat(t).isNull();
  }
}
