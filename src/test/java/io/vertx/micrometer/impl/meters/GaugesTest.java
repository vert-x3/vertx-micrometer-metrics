package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
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
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class GaugesTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  @Test
  public void shouldAliasGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Gauges<LongAdder> gauges = new Gauges<>("my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get("addr1").increment();
    gauges.get("addr1").increment();
    gauges.get("addr2").increment();

    Gauge g = registry.find("my_gauge").tags("address", "1").gauge();
    assertThat(g.value()).isEqualTo(2d);
    g = registry.find("my_gauge").tags("address", "addr1").gauge();
    assertThat(g).isNull();
    g = registry.find("my_gauge").tags("address", "addr2").gauge();
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
    Gauges<LongAdder> gauges = new Gauges<>("my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get("addr1").increment();
    gauges.get("addr1").increment();
    gauges.get("addr2").increment();

    Gauge g = registry.find("my_gauge").tags("address", "_").gauge();
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
    Gauges<LongAdder> gauges = new Gauges<>("my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.EB_ADDRESS);
    gauges.get(customTags, "addr1").increment();

    Gauge g = registry.find("my_gauge").tags("address", "addr1", "k1", "v1", "k2", "v2").gauge();
    assertThat(g.value()).isEqualTo(1d);
  }
}
