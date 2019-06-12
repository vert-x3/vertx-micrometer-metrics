package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.micrometer.MicrometerMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.MicrometerMetricsOptions} original class using Vert.x codegen.
 */
public class MicrometerMetricsOptionsConverter implements JsonCodec<MicrometerMetricsOptions, JsonObject> {

  public static final MicrometerMetricsOptionsConverter INSTANCE = new MicrometerMetricsOptionsConverter();

  @Override public JsonObject encode(MicrometerMetricsOptions value) { return (value != null) ? value.toJson() : null; }

  @Override public MicrometerMetricsOptions decode(JsonObject value) { return (value != null) ? new MicrometerMetricsOptions(value) : null; }

  @Override public Class<MicrometerMetricsOptions> getTargetClass() { return MicrometerMetricsOptions.class; }

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, MicrometerMetricsOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "disabledMetricsCategories":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<io.vertx.micrometer.MetricsDomain> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add(io.vertx.micrometer.MetricsDomain.valueOf((String)item));
            });
            obj.setDisabledMetricsCategories(list);
          }
          break;
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "influxDbOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setInfluxDbOptions(io.vertx.micrometer.VertxInfluxDbOptionsConverter.INSTANCE.decode((JsonObject)member.getValue()));
          }
          break;
        case "jmxMetricsOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setJmxMetricsOptions(io.vertx.micrometer.VertxJmxMetricsOptionsConverter.INSTANCE.decode((JsonObject)member.getValue()));
          }
          break;
        case "jvmMetricsEnabled":
          if (member.getValue() instanceof Boolean) {
            obj.setJvmMetricsEnabled((Boolean)member.getValue());
          }
          break;
        case "labelMatches":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.micrometer.Match> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(io.vertx.micrometer.MatchConverter.INSTANCE.decode((JsonObject)item));
            });
            obj.setLabelMatches(list);
          }
          break;
        case "labelMatchs":
          if (member.getValue() instanceof JsonArray) {
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                obj.addLabelMatch(io.vertx.micrometer.MatchConverter.INSTANCE.decode((JsonObject)item));
            });
          }
          break;
        case "labels":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<io.vertx.micrometer.Label> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add(io.vertx.micrometer.Label.valueOf((String)item));
            });
            obj.setLabels(list);
          }
          break;
        case "prometheusOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setPrometheusOptions(io.vertx.micrometer.VertxPrometheusOptionsConverter.INSTANCE.decode((JsonObject)member.getValue()));
          }
          break;
        case "registryName":
          if (member.getValue() instanceof String) {
            obj.setRegistryName((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(MicrometerMetricsOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(MicrometerMetricsOptions obj, java.util.Map<String, Object> json) {
    if (obj.getDisabledMetricsCategories() != null) {
      JsonArray array = new JsonArray();
      obj.getDisabledMetricsCategories().forEach(item -> array.add(item.name()));
      json.put("disabledMetricsCategories", array);
    }
    json.put("enabled", obj.isEnabled());
    if (obj.getInfluxDbOptions() != null) {
      json.put("influxDbOptions", io.vertx.micrometer.VertxInfluxDbOptionsConverter.INSTANCE.encode(obj.getInfluxDbOptions()));
    }
    if (obj.getJmxMetricsOptions() != null) {
      json.put("jmxMetricsOptions", io.vertx.micrometer.VertxJmxMetricsOptionsConverter.INSTANCE.encode(obj.getJmxMetricsOptions()));
    }
    json.put("jvmMetricsEnabled", obj.isJvmMetricsEnabled());
    if (obj.getLabels() != null) {
      JsonArray array = new JsonArray();
      obj.getLabels().forEach(item -> array.add(item.name()));
      json.put("labels", array);
    }
    if (obj.getPrometheusOptions() != null) {
      json.put("prometheusOptions", io.vertx.micrometer.VertxPrometheusOptionsConverter.INSTANCE.encode(obj.getPrometheusOptions()));
    }
    if (obj.getRegistryName() != null) {
      json.put("registryName", obj.getRegistryName());
    }
  }
}
