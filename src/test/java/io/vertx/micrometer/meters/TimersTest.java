package io.vertx.micrometer.meters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.backend.BackendRegistries;
import io.vertx.micrometer.match.Match;
import io.vertx.micrometer.match.MatchType;
import org.junit.Test;

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
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Timers timers = new Timers("my_timer", "", registry, Label.ADDRESS);
    timers.get("addr1").record(5, TimeUnit.MILLISECONDS);
    timers.get("addr1").record(8, TimeUnit.MILLISECONDS);
    timers.get("addr2").record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.find("my_timer").tags("address", "1").timer();
    assertThat(t.count()).isEqualTo(2);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(13);
    t = registry.find("my_timer").tags("address", "addr1").timer();
    assertThat(t).isNull();
    t = registry.find("my_timer").tags("address", "addr2").timer();
    assertThat(t.count()).isEqualTo(1);
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
    Timers timers = new Timers("my_timer", "", registry, Label.ADDRESS);
    timers.get("addr1").record(5, TimeUnit.MILLISECONDS);
    timers.get("addr1").record(8, TimeUnit.MILLISECONDS);
    timers.get("addr2").record(10, TimeUnit.MILLISECONDS);

    Timer t = registry.find("my_timer").timer();
    assertThat(t.count()).isEqualTo(3);
    assertThat(t.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(23);
    t = registry.find("my_timer").tags("address", "addr1").timer();
    assertThat(t).isNull();
    t = registry.find("my_timer").tags("address", "addr2").timer();
    assertThat(t).isNull();
  }
}
