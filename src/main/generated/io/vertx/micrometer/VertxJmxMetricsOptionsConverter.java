package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.micrometer.VertxJmxMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link "io.vertx.micrometer.VertxJmxMetricsOptions} original class using Vert.x codegen.
 */
public class VertxJmxMetricsOptionsConverter {

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxJmxMetricsOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "domain":
          if (member.getValue() instanceof String) {
            obj.setDomain((String)member.getValue());
          }
          break;
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "step":
          if (member.getValue() instanceof Number) {
            obj.setStep(((Number)member.getValue()).intValue());
          }
          break;
      }
    }
  }

  public static void toJson(VertxJmxMetricsOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(VertxJmxMetricsOptions obj, java.util.Map<String, Object> json) {
    if (obj.getDomain() != null) {
      json.put("domain", obj.getDomain());
    }
    json.put("enabled", obj.isEnabled());
    json.put("step", obj.getStep());
  }
}
