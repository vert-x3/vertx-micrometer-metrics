package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;

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
    Counters counters = new Counters("my_counter", "", registry, Label.EB_ADDRESS);
    counters.get("addr1").increment();
    counters.get("addr1").increment();
    counters.get("addr2").increment();

    Counter c = registry.find("my_counter").tags("address", "1").counter();
    assertThat(c.count()).isEqualTo(2d);
    c = registry.find("my_counter").tags("address", "addr1").counter();
    assertThat(c).isNull();
    c = registry.find("my_counter").tags("address", "addr2").counter();
    assertThat(c.count()).isEqualTo(1d);
  }

  @Test
  public void shouldIgnoreCounterLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Counters counters = new Counters("my_counter", "", registry, Label.EB_ADDRESS);
    counters.get("addr1").increment();
    counters.get("addr1").increment();
    counters.get("addr2").increment();

    Counter c = registry.find("my_counter").tags("address", "_").counter();
    assertThat(c.count()).isEqualTo(3d);
    c = registry.find("my_counter").tags("address", "addr1").counter();
    assertThat(c).isNull();
    c = registry.find("my_counter").tags("address", "addr2").counter();
    assertThat(c).isNull();
  }
}
