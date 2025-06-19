package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.micrometer.VertxPrometheusOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxPrometheusOptions} original class using Vert.x codegen.
 */
public class VertxPrometheusOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxPrometheusOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "embeddedServerEndpoint":
          if (member.getValue() instanceof String) {
            obj.setEmbeddedServerEndpoint((String)member.getValue());
          }
          break;
        case "embeddedServerOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setEmbeddedServerOptions(new io.vertx.core.http.HttpServerOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "publishQuantiles":
          if (member.getValue() instanceof Boolean) {
            obj.setPublishQuantiles((Boolean)member.getValue());
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

   static void toJson(VertxPrometheusOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(VertxPrometheusOptions obj, java.util.Map<String, Object> json) {
    if (obj.getEmbeddedServerEndpoint() != null) {
      json.put("embeddedServerEndpoint", obj.getEmbeddedServerEndpoint());
    }
    if (obj.getEmbeddedServerOptions() != null) {
      json.put("embeddedServerOptions", obj.getEmbeddedServerOptions().toJson());
    }
    json.put("enabled", obj.isEnabled());
    json.put("publishQuantiles", obj.isPublishQuantiles());
    json.put("startEmbeddedServer", obj.isStartEmbeddedServer());
  }
}
