/*
 * Copyright (c) 2014 Red Hat, Inc. and others
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.monitoring.backend;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.monitoring.backend.VertxInfluxDbOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.monitoring.backend.VertxInfluxDbOptions} original class using Vert.x codegen.
 */
public class VertxInfluxDbOptionsConverter {

  public static void fromJson(JsonObject json, VertxInfluxDbOptions obj) {
    if (json.getValue("batchSize") instanceof Number) {
      obj.setBatchSize(((Number)json.getValue("batchSize")).intValue());
    }
    if (json.getValue("compressed") instanceof Boolean) {
      obj.setCompressed((Boolean)json.getValue("compressed"));
    }
    if (json.getValue("connectTimeout") instanceof Number) {
      obj.setConnectTimeout(((Number)json.getValue("connectTimeout")).intValue());
    }
    if (json.getValue("db") instanceof String) {
      obj.setDb((String)json.getValue("db"));
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("numThreads") instanceof Number) {
      obj.setNumThreads(((Number)json.getValue("numThreads")).intValue());
    }
    if (json.getValue("password") instanceof String) {
      obj.setPassword((String)json.getValue("password"));
    }
    if (json.getValue("readTimeout") instanceof Number) {
      obj.setReadTimeout(((Number)json.getValue("readTimeout")).intValue());
    }
    if (json.getValue("retentionPolicy") instanceof String) {
      obj.setRetentionPolicy((String)json.getValue("retentionPolicy"));
    }
    if (json.getValue("step") instanceof Number) {
      obj.setStep(((Number)json.getValue("step")).intValue());
    }
    if (json.getValue("uri") instanceof String) {
      obj.setUri((String)json.getValue("uri"));
    }
    if (json.getValue("userName") instanceof String) {
      obj.setUserName((String)json.getValue("userName"));
    }
  }

  public static void toJson(VertxInfluxDbOptions obj, JsonObject json) {
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