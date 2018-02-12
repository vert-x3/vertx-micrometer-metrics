package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.Gauge;
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
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class GaugesTest {

  @Test
  public void shouldAliasGaugeLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Gauges<LongAdder> gauges = new Gauges<>(MetricsCategory.VERTICLES, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.ADDRESS);
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
    gauges.get(labelMatchers, "addr1").increment();
    gauges.get(labelMatchers, "addr1").increment();
    gauges.get(labelMatchers, "addr2").increment();

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
    Gauges<LongAdder> gauges = new Gauges<>(MetricsCategory.VERTICLES, "my_gauge", "", LongAdder::new, LongAdder::doubleValue, registry, Label.ADDRESS);
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(new Match()
      .setDomain(MetricsCategory.VERTICLES)
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    gauges.get(labelMatchers, "addr1").increment();
    gauges.get(labelMatchers, "addr1").increment();
    gauges.get(labelMatchers, "addr2").increment();

    Gauge g = registry.find("my_gauge").tags("address", "_").gauge();
    assertThat(g.value()).isEqualTo(3d);
    g = registry.find("my_gauge").tags("address", "addr1").gauge();
    assertThat(g).isNull();
    g = registry.find("my_gauge").tags("address", "addr2").gauge();
    assertThat(g).isNull();
  }
}
