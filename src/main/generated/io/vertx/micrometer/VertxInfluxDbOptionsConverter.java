package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.micrometer.VertxInfluxDbOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxInfluxDbOptions} original class using Vert.x codegen.
 */
public class VertxInfluxDbOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxInfluxDbOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "batchSize":
          if (member.getValue() instanceof Number) {
            obj.setBatchSize(((Number)member.getValue()).intValue());
          }
          break;
        case "compressed":
          if (member.getValue() instanceof Boolean) {
            obj.setCompressed((Boolean)member.getValue());
          }
          break;
        case "connectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setConnectTimeout(((Number)member.getValue()).intValue());
          }
          break;
        case "db":
          if (member.getValue() instanceof String) {
            obj.setDb((String)member.getValue());
          }
          break;
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "numThreads":
          if (member.getValue() instanceof Number) {
            obj.setNumThreads(((Number)member.getValue()).intValue());
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "readTimeout":
          if (member.getValue() instanceof Number) {
            obj.setReadTimeout(((Number)member.getValue()).intValue());
          }
          break;
        case "retentionPolicy":
          if (member.getValue() instanceof String) {
            obj.setRetentionPolicy((String)member.getValue());
          }
          break;
        case "step":
          if (member.getValue() instanceof Number) {
            obj.setStep(((Number)member.getValue()).intValue());
          }
          break;
        case "uri":
          if (member.getValue() instanceof String) {
            obj.setUri((String)member.getValue());
          }
          break;
        case "userName":
          if (member.getValue() instanceof String) {
            obj.setUserName((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(VertxInfluxDbOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(VertxInfluxDbOptions obj, java.util.Map<String, Object> json) {
    json.put("batchSize", obj.getBatchSize());
    json.put("compressed", obj.isCompressed());
    json.put("connectTimeout", obj.getConnectTimeout());
    if (obj.getDb() != null) {
      json.put("db", obj.getDb());
    }
    json.put("enabled", obj.isEnabled());
    json.put("numThreads", obj.getNumThreads());
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    json.put("readTimeout", obj.getReadTimeout());
    if (obj.getRetentionPolicy() != null) {
      json.put("retentionPolicy", obj.getRetentionPolicy());
    }
    json.put("step", obj.getStep());
    if (obj.getUri() != null) {
      json.put("uri", obj.getUri());
    }
    if (obj.getUserName() != null) {
      json.put("userName", obj.getUserName());
    }
  }
}
