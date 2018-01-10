/*
 * Copyright 2014 Red Hat, Inc.
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

package io.vertx.ext.monitoring.base;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.ext.monitoring.base.MetricsOptionsBase}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.ext.monitoring.base.MetricsOptionsBase} original class using Vert.x codegen.
 */
public class MetricsOptionsBaseConverter {

  public static void fromJson(JsonObject json, MetricsOptionsBase obj) {
    if (json.getValue("disabledMetricsTypes") instanceof JsonArray) {
      java.util.HashSet<io.vertx.ext.monitoring.base.MetricsType> list = new java.util.HashSet<>();
      json.getJsonArray("disabledMetricsTypes").forEach( item -> {
        if (item instanceof String)
          list.add(io.vertx.ext.monitoring.base.MetricsType.valueOf((String)item));
      });
      obj.setDisabledMetricsTypes(list);
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("metricsBridgeAddress") instanceof String) {
      obj.setMetricsBridgeAddress((String)json.getValue("metricsBridgeAddress"));
    }
    if (json.getValue("metricsBridgeEnabled") instanceof Boolean) {
      obj.setMetricsBridgeEnabled((Boolean)json.getValue("metricsBridgeEnabled"));
    }
  }

  public static void toJson(MetricsOptionsBase obj, JsonObject json) {
    if (obj.getDisabledMetricsTypes() != null) {
      JsonArray array = new JsonArray();
      obj.getDisabledMetricsTypes().forEach(item -> array.add(item.name()));
      json.put("disabledMetricsTypes", array);
    }
    json.put("enabled", obj.isEnabled());
    if (obj.getMetricsBridgeAddress() != null) {
      json.put("metricsBridgeAddress", obj.getMetricsBridgeAddress());
    }
    json.put("metricsBridgeEnabled", obj.isMetricsBridgeEnabled());
  }
}