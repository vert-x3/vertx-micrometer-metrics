package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.micrometer.VertxJmxMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxJmxMetricsOptions} original class using Vert.x codegen.
 */
public class VertxJmxMetricsOptionsConverter {

  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
  private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxJmxMetricsOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "domain":
          if (member.getValue() instanceof String) {
            obj.setDomain((String)member.getValue());
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

   static void toJson(VertxJmxMetricsOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(VertxJmxMetricsOptions obj, java.util.Map<String, Object> json) {
    json.put("enabled", obj.isEnabled());
    if (obj.getDomain() != null) {
      json.put("domain", obj.getDomain());
    }
    json.put("step", obj.getStep());
  }
}
