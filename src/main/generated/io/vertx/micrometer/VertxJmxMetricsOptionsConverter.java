package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.micrometer.VertxJmxMetricsOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxJmxMetricsOptions} original class using Vert.x codegen.
 */
public class VertxJmxMetricsOptionsConverter implements JsonCodec<VertxJmxMetricsOptions, JsonObject> {

  public static final VertxJmxMetricsOptionsConverter INSTANCE = new VertxJmxMetricsOptionsConverter();

  @Override public JsonObject encode(VertxJmxMetricsOptions value) { return (value != null) ? value.toJson() : null; }

  @Override public VertxJmxMetricsOptions decode(JsonObject value) { return (value != null) ? new VertxJmxMetricsOptions(value) : null; }

  @Override public Class<VertxJmxMetricsOptions> getTargetClass() { return VertxJmxMetricsOptions.class; }

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
