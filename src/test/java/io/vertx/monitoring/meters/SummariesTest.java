package io.vertx.monitoring.meters;

import io.micrometer.core.instrument.DistributionSummary;
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
public class SummariesTest {

  @Test
  public void shouldAliasSummaryLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Summaries summaries = new Summaries(MetricsCategory.VERTICLES, "my_summary", "", registry, Label.ADDRESS);
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
    summaries.get(labelMatchers, "addr1").record(5);
    summaries.get(labelMatchers, "addr1").record(8);
    summaries.get(labelMatchers, "addr2").record(10);

    DistributionSummary s = registry.summary("my_summary", "address", "1");
    assertThat(s.count()).isEqualTo(2);
    assertThat(s.totalAmount()).isEqualTo(13);
    s = registry.summary("my_summary", "address", "addr1");
    assertThat(s.count()).isEqualTo(0);
    assertThat(s.totalAmount()).isEqualTo(0);
    s = registry.summary("my_summary", "address", "addr2");
    assertThat(s.count()).isEqualTo(1);
    assertThat(s.totalAmount()).isEqualTo(10);
  }

  @Test
  public void shouldIgnoreSummaryLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    Summaries summaries = new Summaries(MetricsCategory.VERTICLES, "my_summary", "", registry, Label.ADDRESS);
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(new Match()
      .setDomain(MetricsCategory.VERTICLES)
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    summaries.get(labelMatchers, "addr1").record(5);
    summaries.get(labelMatchers, "addr1").record(8);
    summaries.get(labelMatchers, "addr2").record(10);

    DistributionSummary s = registry.summary("my_summary", "address", "_");
    assertThat(s.count()).isEqualTo(3);
    assertThat(s.totalAmount()).isEqualTo(23);
    s = registry.summary("my_summary", "address", "addr1");
    assertThat(s.count()).isEqualTo(0);
    assertThat(s.totalAmount()).isEqualTo(0);
    s = registry.summary("my_summary", "address", "addr2");
    assertThat(s.count()).isEqualTo(0);
    assertThat(s.totalAmount()).isEqualTo(0);
  }
}
