package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.micrometer.impl.Label;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import org.junit.Test;

import java.util.Collections;

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
    Summaries summaries = new Summaries("my_summary", "", registry, Label.ADDRESS);
    summaries.get("addr1").record(5);
    summaries.get("addr1").record(8);
    summaries.get("addr2").record(10);

    DistributionSummary s = registry.find("my_summary").tags("address", "1").summary();
    assertThat(s.count()).isEqualTo(2);
    assertThat(s.totalAmount()).isEqualTo(13);
    s = registry.find("my_summary").tags("address", "addr1").summary();
    assertThat(s).isNull();
    s = registry.find("my_summary").tags("address", "addr2").summary();
    assertThat(s.count()).isEqualTo(1);
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
    Summaries summaries = new Summaries("my_summary", "", registry, Label.ADDRESS);
    summaries.get("addr1").record(5);
    summaries.get("addr1").record(8);
    summaries.get("addr2").record(10);

    DistributionSummary s = registry.find("my_summary").tags("address", "_").summary();
    assertThat(s.count()).isEqualTo(3);
    assertThat(s.totalAmount()).isEqualTo(23);
    s = registry.find("my_summary").tags("address", "addr1").summary();
    assertThat(s).isNull();
    s = registry.find("my_summary").tags("address", "addr2").summary();
    assertThat(s).isNull();
  }
}
