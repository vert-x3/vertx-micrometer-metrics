package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.micrometer.MicrometerMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link "io.vertx.micrometer.MicrometerMetricsOptions} original class using Vert.x codegen.
 */
public class MicrometerMetricsOptionsConverter {

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
            obj.setInfluxDbOptions(new io.vertx.micrometer.VertxInfluxDbOptions((JsonObject)member.getValue()));
          }
          break;
        case "jmxMetricsOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setJmxMetricsOptions(new io.vertx.micrometer.VertxJmxMetricsOptions((JsonObject)member.getValue()));
          }
          break;
        case "labelMatchs":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.micrometer.Match> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.micrometer.Match((JsonObject)item));
            });
            obj.setLabelMatchs(list);
          }
          break;
        case "prometheusOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setPrometheusOptions(new io.vertx.micrometer.VertxPrometheusOptions((JsonObject)member.getValue()));
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
    if (obj.getRegistryName() != null) {
      json.put("registryName", obj.getRegistryName());
    }
  }
}
