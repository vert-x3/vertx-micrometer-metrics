package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.micrometer.MicrometerMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.MicrometerMetricsOptions} original class using Vert.x codegen.
 */
public class MicrometerMetricsOptionsConverter {

  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
  private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, MicrometerMetricsOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "disabledMetricsCategories":
          if (member.getValue() instanceof JsonArray) {
            java.util.LinkedHashSet<java.lang.String> list =  new java.util.LinkedHashSet<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setDisabledMetricsCategories(list);
          }
          break;
        case "registryName":
          if (member.getValue() instanceof String) {
            obj.setRegistryName((String)member.getValue());
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
        case "labelMatches":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.micrometer.Match> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.micrometer.Match((io.vertx.core.json.JsonObject)item));
            });
            obj.setLabelMatches(list);
          }
          break;
        case "labelMatchs":
          if (member.getValue() instanceof JsonArray) {
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                obj.addLabelMatch(new io.vertx.micrometer.Match((io.vertx.core.json.JsonObject)item));
            });
          }
          break;
        case "influxDbOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setInfluxDbOptions(new io.vertx.micrometer.VertxInfluxDbOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "prometheusOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setPrometheusOptions(new io.vertx.micrometer.VertxPrometheusOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "jmxMetricsOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setJmxMetricsOptions(new io.vertx.micrometer.VertxJmxMetricsOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "jvmMetricsEnabled":
          if (member.getValue() instanceof Boolean) {
            obj.setJvmMetricsEnabled((Boolean)member.getValue());
          }
          break;
        case "nettyMetricsEnabled":
          if (member.getValue() instanceof Boolean) {
            obj.setNettyMetricsEnabled((Boolean)member.getValue());
          }
          break;
        case "metricsNaming":
          if (member.getValue() instanceof JsonObject) {
            obj.setMetricsNaming(new io.vertx.micrometer.MetricsNaming((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "meterCacheEnabled":
          if (member.getValue() instanceof Boolean) {
            obj.setMeterCacheEnabled((Boolean)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(MicrometerMetricsOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(MicrometerMetricsOptions obj, java.util.Map<String, Object> json) {
    json.put("enabled", obj.isEnabled());
    if (obj.getDisabledMetricsCategories() != null) {
      JsonArray array = new JsonArray();
      obj.getDisabledMetricsCategories().forEach(item -> array.add(item));
      json.put("disabledMetricsCategories", array);
    }
    if (obj.getRegistryName() != null) {
      json.put("registryName", obj.getRegistryName());
    }
    if (obj.getLabels() != null) {
      JsonArray array = new JsonArray();
      obj.getLabels().forEach(item -> array.add(item.name()));
      json.put("labels", array);
    }
    if (obj.getInfluxDbOptions() != null) {
      json.put("influxDbOptions", obj.getInfluxDbOptions().toJson());
    }
    if (obj.getPrometheusOptions() != null) {
      json.put("prometheusOptions", obj.getPrometheusOptions().toJson());
    }
    if (obj.getJmxMetricsOptions() != null) {
      json.put("jmxMetricsOptions", obj.getJmxMetricsOptions().toJson());
    }
    json.put("jvmMetricsEnabled", obj.isJvmMetricsEnabled());
    json.put("nettyMetricsEnabled", obj.isNettyMetricsEnabled());
    if (obj.getMetricsNaming() != null) {
      json.put("metricsNaming", obj.getMetricsNaming().toJson());
    }
    json.put("meterCacheEnabled", obj.isMeterCacheEnabled());
  }
}
