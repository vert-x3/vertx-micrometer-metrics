package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter for {@link io.vertx.micrometer.VertxPrometheusOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxPrometheusOptions} original class using Vert.x codegen.
 */
public class VertxPrometheusOptionsConverter {

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxPrometheusOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "embeddedServerEndpoint":
          if (member.getValue() instanceof String) {
            obj.setEmbeddedServerEndpoint((String)member.getValue());
          }
          break;
        case "embeddedServerOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setEmbeddedServerOptions(new io.vertx.core.http.HttpServerOptions((JsonObject)member.getValue()));
          }
          break;
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "startEmbeddedServer":
          if (member.getValue() instanceof Boolean) {
            obj.setStartEmbeddedServer((Boolean)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(VertxPrometheusOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(VertxPrometheusOptions obj, java.util.Map<String, Object> json) {
    if (obj.getEmbeddedServerEndpoint() != null) {
      json.put("embeddedServerEndpoint", obj.getEmbeddedServerEndpoint());
    }
    if (obj.getEmbeddedServerOptions() != null) {
      json.put("embeddedServerOptions", obj.getEmbeddedServerOptions().toJson());
    }
    json.put("enabled", obj.isEnabled());
    json.put("startEmbeddedServer", obj.isStartEmbeddedServer());
  }
}
