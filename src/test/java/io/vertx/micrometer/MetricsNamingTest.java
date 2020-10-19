package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsNamingTest {

  private static int NB_METRICS = 39;

  @Test
  public void v3NamesShouldCoverAllMetrics() {
    MetricsNaming names = MetricsNaming.v3Names();
    JsonObject json = names.toJson();
    assertThat(json.size()).isEqualTo(NB_METRICS);
    json.forEach(entry -> assertThat(entry.getValue()).isNotNull());
  }

  @Test
  public void v4NamesShouldCoverAllMetrics() {
    MetricsNaming names = MetricsNaming.v4Names();
    JsonObject json = names.toJson();
    assertThat(json.size()).isEqualTo(NB_METRICS);
    json.forEach(entry -> assertThat(entry.getValue()).isNotNull());
  }

  @Test
  public void copyCtorShouldCoverAllMetrics() {
    MetricsNaming names = new MetricsNaming(MetricsNaming.v4Names());
    JsonObject json = names.toJson();
    assertThat(json.size()).isEqualTo(NB_METRICS);
    json.forEach(entry -> assertThat(entry.getValue()).isNotNull());
  }
}
