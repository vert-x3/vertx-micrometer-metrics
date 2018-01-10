package io.vertx.monitoring.match;

import io.micrometer.core.instrument.Tag;
import io.vertx.monitoring.MetricsCategory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
public class LabelMatchersTest {
  private static final String[] KEYS = {"local", "remote"};
  private static final String[] METRIC1_VALUES = {"whatever", "cartago"};
  private static final String[] METRIC2_VALUES = {"whatever", "rio"};
  private static final String[] METRIC3_VALUES = {"whatever", "tiahuanaco"};
  private static final String[] METRIC4_VALUES = {"whatever", "rio"};
  private static final Match MATCH1 = new Match().setLabel("remote").setType(MatchType.EQUALS).setValue("rio");
  private static final Match MATCH2 = new Match().setLabel("remote").setType(MatchType.REGEX).setValue(".*");
  private static final Match MATCH3 = new Match().setDomain(MetricsCategory.HTTP_SERVER)
    .setLabel("remote").setType(MatchType.EQUALS).setValue("rio");
  private static final Match MATCH4 = new Match().setDomain(MetricsCategory.HTTP_SERVER)
    .setLabel("remote").setType(MatchType.REGEX).setValue(".*");
  private static final Match MATCH5 = new Match().setDomain(MetricsCategory.HTTP_SERVER)
    .setLabel("remote").setType(MatchType.EQUALS).setValue("cartago");
  private static final Match MATCH6 = new Match().setDomain(MetricsCategory.HTTP_CLIENT)
    .setLabel("remote").setType(MatchType.EQUALS).setValue("tiahuanaco");
  private static final Match MATCH_ALIAS_1 = new Match().setLabel("remote").setValue("tiahuanaco").setAlias("Tiwanaku");
  private static final Match MATCH_ALIAS_2 = new Match().setDomain(MetricsCategory.HTTP_CLIENT)
    .setLabel("remote").setValue("tiahuanaco").setAlias("tia");

  @Test
  public void shouldFilterStrictNoDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(MATCH1));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).isNull();
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).isNull();
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldFilterRegexNoDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(MATCH2));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldFilterStrictWithDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(MATCH3));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).isNull();
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldFilterRegexWithDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Collections.singletonList(MATCH4));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldMatchBroadestNoDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH1, MATCH2));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldCombineMatches() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH1, MATCH4));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).isNull();
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldMatchBroadestWithMixedDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH2, MATCH3));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldMatchBroadestWithDomain() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH3, MATCH4));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "rio"));
  }

  @Test
  public void shouldMatchSeveralDomains() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH5, MATCH6));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "cartago"));
    assertThat(rs.metric2Result).isNull();
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tiahuanaco"));
    assertThat(rs.metric4Result).isNull();
  }

  @Test
  public void shouldDomainMatchTakePrecedence() {
    LabelMatchers labelMatchers = new LabelMatchers(Arrays.asList(MATCH_ALIAS_1, MATCH_ALIAS_2));
    ResultSet rs = run(labelMatchers);
    assertThat(rs.metric1Result).isNull();
    assertThat(rs.metric2Result).isNull();
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tia"));
    assertThat(rs.metric4Result).isNull();

    // Same with reverse input matches order
    labelMatchers = new LabelMatchers(Arrays.asList(MATCH_ALIAS_2, MATCH_ALIAS_1));
    rs = run(labelMatchers);
    assertThat(rs.metric1Result).isNull();
    assertThat(rs.metric2Result).isNull();
    assertThat(rs.metric3Result).containsExactly(Tag.of("local", "whatever"), Tag.of("remote", "tia"));
    assertThat(rs.metric4Result).isNull();
  }

  private static ResultSet run(LabelMatchers labelMatchers) {
    ResultSet rs = new ResultSet();
    rs.metric1Result = labelMatchers.toTags(MetricsCategory.HTTP_SERVER, KEYS, METRIC1_VALUES);
    rs.metric2Result = labelMatchers.toTags(MetricsCategory.HTTP_SERVER, KEYS, METRIC2_VALUES);
    rs.metric3Result = labelMatchers.toTags(MetricsCategory.HTTP_CLIENT, KEYS, METRIC3_VALUES);
    rs.metric4Result = labelMatchers.toTags(MetricsCategory.HTTP_CLIENT, KEYS, METRIC4_VALUES);
    return rs;
  }

  private static class ResultSet {
    private List<Tag> metric1Result;
    private List<Tag> metric2Result;
    private List<Tag> metric3Result;
    private List<Tag> metric4Result;
  }
}
