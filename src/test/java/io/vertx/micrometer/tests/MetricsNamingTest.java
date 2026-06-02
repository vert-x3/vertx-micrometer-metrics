package io.vertx.micrometer.tests;

import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MetricsNaming;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsNamingTest {

  private static final int NB_METRICS = 39;

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
