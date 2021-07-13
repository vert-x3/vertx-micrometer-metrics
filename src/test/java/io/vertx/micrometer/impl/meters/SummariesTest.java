package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.DistributionSummary;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class SummariesTest {

  private static final EnumSet<Label> ALL_LABELS = EnumSet.allOf(Label.class);

  @Test
  public void shouldAliasSummaryLabel() {
    MeterRegistry registry = new SimpleMeterRegistry();
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue("addr1")
      .setAlias("1")));
    Summaries summaries = new Summaries("my_summary", "", registry, Label.EB_ADDRESS);
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
    BackendRegistries.registerMatchers(registry, ALL_LABELS, Collections.singletonList(new Match()
      .setLabel("address")
      .setType(MatchType.REGEX)
      .setValue(".*")
      .setAlias("_")));
    Summaries summaries = new Summaries("my_summary", "", registry, Label.EB_ADDRESS);
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

  @Test
  public void shouldAddCustomTags() {
    List<Tag> customTags = Arrays.asList(Tag.of("k1", "v1"), Tag.of("k2", "v2"));
    MeterRegistry registry = new SimpleMeterRegistry();
    Summaries summaries = new Summaries("my_summary", "", registry, Label.EB_ADDRESS);
    summaries.get(customTags, "addr1").record(5);

    DistributionSummary s = registry.find("my_summary").tags("address", "addr1", "k1", "v1", "k2", "v2").summary();
    assertThat(s.count()).isEqualTo(1);
  }
}
