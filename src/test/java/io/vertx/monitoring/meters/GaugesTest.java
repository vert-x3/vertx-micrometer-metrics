package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.monitoring.Label;
import io.vertx.monitoring.backend.BackendRegistries;
import io.vertx.monitoring.match.Match;
import io.vertx.monitoring.match.MatchType;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class GaugesTest {

  @Test
  public void shouldAliasGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Gauges<LongAdder> gauges = new Gauges<>("my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.ADDRESS);
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
    BackendRegistries.registerMatchers(registry, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Gauges<LongAdder> gauges = new Gauges<>("my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.ADDRESS);
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
}
