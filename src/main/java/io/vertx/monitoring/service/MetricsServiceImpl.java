package io.vertx.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.HistogramSnapshot;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.Measured;
import io.vertx.core.spi.metrics.Metrics;
import io.vertx.core.spi.metrics.MetricsProvider;
import io.vertx.monitoring.MicrometerMetrics;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author Joel Takvorian
 */
public class MetricsServiceImpl implements MetricsService {

  private final MicrometerMetrics metrics;

  public MetricsServiceImpl(Measured measured) {
    MetricsProvider provider = (MetricsProvider) measured;
    Metrics baseMetrics = provider.getMetrics();
    if (baseMetrics instanceof MicrometerMetrics) {
      metrics = (MicrometerMetrics) baseMetrics;
    } else {
      metrics = null;
    }
  }

  @Override
  public String getBaseName() {
    return metrics != null ? metrics.baseName() : null;
  }

  @Override
  public JsonObject getMetricsSnapshot() {
    return metrics != null ? snapshot(metrics.registry(), metrics.baseName()) : null;
  }

  @Override
  public JsonObject getMetricsSnapshot(String baseName) {
    return metrics != null ? snapshot(metrics.registry(), baseName) : null;
  }

  @Override
  public Set<String> metricsNames() {
    if (metrics != null) {
      return metrics.registry().getMeters().stream()
        .map(m -> m.getId().getName())
        .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  /**
   * @return the Micrometer registry used for these metrics, null if no metrics is available.
   */
  public MeterRegistry getRegistry() {
    return metrics != null ? metrics.registry() : null;
  }

  private JsonObject snapshot(MeterRegistry registry, String baseName) {
    Stream<Meter> filtered;
    if (baseName == null) {
      filtered = registry.getMeters().stream();
    } else {
      filtered = registry.getMeters().stream()
        .filter(m -> m.getId().getName().startsWith(baseName));
    }
    LinkedHashMap<String, List<JsonObject>> map = filtered
      .sorted(Comparator.comparing(m -> m.getId().getName()))
      .collect(
        Collectors.groupingBy(m -> m.getId().getName(),
          LinkedHashMap::new,
          Collectors.mapping(MetricsServiceImpl::metricToJson, Collectors.toList())));
    return new JsonObject((Map<String, Object>)(Object) map);
  }

  private static JsonObject metricToJson(Meter meter) {
    JsonObject tags = new JsonObject();
    meter.getId().getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
    JsonObject obj = new JsonObject().put("tags", tags);
    switch (meter.getId().getType()) {
      case Counter:
        return counterToJson(obj, (Counter)meter);
      case Gauge:
        return gaugeToJson(obj, (Gauge)meter);
      case Timer:
        return timerToJson(obj, (Timer)meter);
      case DistributionSummary:
        return summaryToJson(obj, (DistributionSummary)meter);
      case Other:
      default:
        return obj.put("type", "unknown");
    }
  }

  private static JsonObject summaryToJson(JsonObject obj, DistributionSummary summary) {
    HistogramSnapshot snapshot = summary.takeSnapshot(false);
    return obj.put("type", "summary")
      .put("count", snapshot.count())
      .put("total", snapshot.total())
      .put("mean", snapshot.mean())
      .put("max", snapshot.max());
  }

  private static JsonObject timerToJson(JsonObject obj, Timer timer) {
    return obj.put("type", "timer")
      .put("count", timer.count())
      .put("totalTimeMs", timer.totalTime(TimeUnit.MILLISECONDS))
      .put("meanMs", timer.mean(TimeUnit.MILLISECONDS))
      .put("maxMs", timer.max(TimeUnit.MILLISECONDS));
  }

  private static JsonObject gaugeToJson(JsonObject obj, Gauge gauge) {
    return obj.put("type", "gauge")
      .put("value", gauge.value());
  }

  private static JsonObject counterToJson(JsonObject obj, Counter counter) {
    return obj.put("type", "counter")
      .put("count", counter.count());
  }
}
