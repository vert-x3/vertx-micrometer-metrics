package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.monitoring.Label;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.match.Match;
import io.vertx.monitoring.match.MatchType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class CountersTest {

  @Test
  public void shouldAliasCounterLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Counters counters = new Counters(MetricsCategory.VERTICLES, "my_counter", "", registry, Label.ADDRESS);
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(new Match()
        .setDomain(MetricsCategory.VERTICLES)
        .setLabel("address")
        .setType(MatchType.REGEX)
        .setValue("addr1")
        .setAlias("1"),
      new Match().setDomain(MetricsCategory.VERTICLES)
        .setLabel("address")
        .setType(MatchType.REGEX)
        .setValue(".*")));
    counters.get(labelMatchers, "addr1").increment();
    counters.get(labelMatchers, "addr1").increment();
    counters.get(labelMatchers, "addr2").increment();

    Counter c = registry.counter("my_counter", "address", "1");
    assertThat(c.count()).isEqualTo(2d);
    c = registry.counter("my_counter", "address", "addr1");
    assertThat(c.count()).isEqualTo(0d);
    c = registry.counter("my_counter", "address", "addr2");
    assertThat(c.count()).isEqualTo(1d);
  }

  @Test
  public void shouldIgnoreCounterLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Counters counters = new Counters(MetricsCategory.VERTICLES, "my_counter", "", registry, Label.ADDRESS);
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(new Match()
      .setDomain(MetricsCategory.VERTICLES)
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    counters.get(labelMatchers, "addr1").increment();
    counters.get(labelMatchers, "addr1").increment();
    counters.get(labelMatchers, "addr2").increment();

    Counter c = registry.counter("my_counter", "address", "_");
    assertThat(c.count()).isEqualTo(3d);
    c = registry.counter("my_counter", "address", "addr1");
    assertThat(c.count()).isEqualTo(0d);
    c = registry.counter("my_counter", "address", "addr2");
    assertThat(c.count()).isEqualTo(0d);
  }
}
