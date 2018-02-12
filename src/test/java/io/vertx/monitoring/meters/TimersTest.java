package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.monitoring.Label;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.match.Match;
import io.vertx.monitoring.match.MatchType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class TimersTest {

  @Test
  public void shouldAliasTimerLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Timers timers = new Timers(MetricsCategory.VERTICLES, "my_timer", "", registry, Label.ADDRESS);
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
    timers.get(labelMatchers, "addr1").record(5, TimeUnit.MILLISECONDS);
    timers.get(labelMatchers, "addr1").record(8, TimeUnit.MILLISECONDS);
    timers.get(labelMatchers, "addr2").record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.timer("my_timer", "address", "1");
    assertThat(t.count()).isEqualTo(2);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(13);
    t = registry.timer("my_timer", "address", "addr1");
    assertThat(t.count()).isEqualTo(0);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(0);
    t = registry.timer("my_timer", "address", "addr2");
    assertThat(t.count()).isEqualTo(1);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(10);
  }

  @Test
  public void shouldIgnoreTimerLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Timers timers = new Timers(MetricsCategory.VERTICLES, "my_timer", "", registry, Label.ADDRESS);
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(new Match()
      .setDomain(MetricsCategory.VERTICLES)
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    timers.get(labelMatchers, "addr1").record(5, TimeUnit.MILLISECONDS);
    timers.get(labelMatchers, "addr1").record(8, TimeUnit.MILLISECONDS);
    timers.get(labelMatchers, "addr2").record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.timer("my_timer", "address", "_");
    assertThat(t.count()).isEqualTo(3);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(23);
    t = registry.timer("my_timer", "address", "addr1");
    assertThat(t.count()).isEqualTo(0);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(0);
    t = registry.timer("my_timer", "address", "addr2");
    assertThat(t.count()).isEqualTo(0);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(0);
  }
}
