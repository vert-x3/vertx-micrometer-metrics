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

package io.vertx.monitoring;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.monitoring.VertxMonitoringOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.monitoring.VertxMonitoringOptions} original class using Vert.x codegen.
 */
public class VertxMonitoringOptionsConverter {

  public static void fromJson(JsonObject json, VertxMonitoringOptions obj) {
    if (json.getValue("disabledMetricsCategories") instanceof JsonArray) {
      java.util.LinkedHashSet<io.vertx.monitoring.MetricsDomain> list = new java.util.LinkedHashSet<>();
      json.getJsonArray("disabledMetricsCategories").forEach( item -> {
        if (item instanceof String)
          list.add(io.vertx.monitoring.MetricsDomain.valueOf((String)item));
      });
      obj.setDisabledMetricsCategories(list);
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("influxDbOptions") instanceof JsonObject) {
      obj.setInfluxDbOptions(new io.vertx.monitoring.backend.VertxInfluxDbOptions((JsonObject)json.getValue("influxDbOptions")));
    }
    if (json.getValue("jmxMetricsOptions") instanceof JsonObject) {
      obj.setJmxMetricsOptions(new io.vertx.monitoring.backend.VertxJmxMetricsOptions((JsonObject)json.getValue("jmxMetricsOptions")));
    }
    if (json.getValue("labelMatchs") instanceof JsonArray) {
      json.getJsonArray("labelMatchs").forEach(item -> {
        if (item instanceof JsonObject)
          obj.addLabelMatch(new io.vertx.monitoring.match.Match((JsonObject)item));
      });
    }
    if (json.getValue("prometheusOptions") instanceof JsonObject) {
      obj.setPrometheusOptions(new io.vertx.monitoring.backend.VertxPrometheusOptions((JsonObject)json.getValue("prometheusOptions")));
    }
    if (json.getValue("registryName") instanceof String) {
      obj.setRegistryName((String)json.getValue("registryName"));
    }
  }

  public static void toJson(VertxMonitoringOptions obj, JsonObject json) {
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