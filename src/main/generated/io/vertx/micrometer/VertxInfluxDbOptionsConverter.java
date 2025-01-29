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

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, VertxInfluxDbOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "enabled":
          if (member.getValue() instanceof Boolean) {
            obj.setEnabled((Boolean)member.getValue());
          }
          break;
        case "uri":
          if (member.getValue() instanceof String) {
            obj.setUri((String)member.getValue());
          }
          break;
        case "db":
          if (member.getValue() instanceof String) {
            obj.setDb((String)member.getValue());
          }
          break;
        case "userName":
          if (member.getValue() instanceof String) {
            obj.setUserName((String)member.getValue());
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "retentionPolicy":
          if (member.getValue() instanceof String) {
            obj.setRetentionPolicy((String)member.getValue());
          }
          break;
        case "compressed":
          if (member.getValue() instanceof Boolean) {
            obj.setCompressed((Boolean)member.getValue());
          }
          break;
        case "step":
          if (member.getValue() instanceof Number) {
            obj.setStep(((Number)member.getValue()).intValue());
          }
          break;
        case "connectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setConnectTimeout(((Number)member.getValue()).intValue());
          }
          break;
        case "readTimeout":
          if (member.getValue() instanceof Number) {
            obj.setReadTimeout(((Number)member.getValue()).intValue());
          }
          break;
        case "batchSize":
          if (member.getValue() instanceof Number) {
            obj.setBatchSize(((Number)member.getValue()).intValue());
          }
          break;
        case "org":
          if (member.getValue() instanceof String) {
            obj.setOrg((String)member.getValue());
          }
          break;
        case "bucket":
          if (member.getValue() instanceof String) {
            obj.setBucket((String)member.getValue());
          }
          break;
        case "token":
          if (member.getValue() instanceof String) {
            obj.setToken((String)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(VertxInfluxDbOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(VertxInfluxDbOptions obj, java.util.Map<String, Object> json) {
    json.put("enabled", obj.isEnabled());
    if (obj.getUri() != null) {
      json.put("uri", obj.getUri());
    }
    if (obj.getDb() != null) {
      json.put("db", obj.getDb());
    }
    if (obj.getUserName() != null) {
      json.put("userName", obj.getUserName());
    }
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    if (obj.getRetentionPolicy() != null) {
      json.put("retentionPolicy", obj.getRetentionPolicy());
    }
    json.put("compressed", obj.isCompressed());
    json.put("step", obj.getStep());
    json.put("connectTimeout", obj.getConnectTimeout());
    json.put("readTimeout", obj.getReadTimeout());
    json.put("batchSize", obj.getBatchSize());
    if (obj.getOrg() != null) {
      json.put("org", obj.getOrg());
    }
    if (obj.getBucket() != null) {
      json.put("bucket", obj.getBucket());
    }
    if (obj.getToken() != null) {
      json.put("token", obj.getToken());
    }
  }
}
