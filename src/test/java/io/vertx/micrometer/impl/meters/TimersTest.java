package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class TimersTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  @Test
  public void shouldAliasTimerLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Timers timers = new Timers("my_timer", "", registry, Label.EB_ADDRESS);
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
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Timers timers = new Timers("my_timer", "", registry, Label.EB_ADDRESS);
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

  @Test
  public void shouldAddCustomTags() {
    List<Tag> customTags = Arrays.asList(Tag.of("k1", "v1"), Tag.of("k2", "v2"));
    MeterRegistry registry = new SimpleMeterRegistry();
    Timers timers = new Timers("my_timer", "", registry, Label.EB_ADDRESS);
    timers.get(customTags, "addr1").record(5, TimeUnit.MILLISECONDS);

    Timer t = registry.find("my_timer").tags("address", "addr1", "k1", "v1", "k2", "v2").timer();
    assertThat(t).isNotNull();
  }
}
