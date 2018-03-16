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

package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.micrometer.VertxJmxMetricsOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.VertxJmxMetricsOptions} original class using Vert.x codegen.
 */
public class VertxJmxMetricsOptionsConverter {

  public static void fromJson(JsonObject json, VertxJmxMetricsOptions obj) {
    if (json.getValue("domain") instanceof String) {
      obj.setDomain((String)json.getValue("domain"));
    }
    if (json.getValue("enabled") instanceof Boolean) {
      obj.setEnabled((Boolean)json.getValue("enabled"));
    }
    if (json.getValue("step") instanceof Number) {
      obj.setStep(((Number)json.getValue("step")).intValue());
    }
  }

  public static void toJson(VertxJmxMetricsOptions obj, JsonObject json) {
    if (obj.getDomain() != null) {
      json.put("domain", obj.getDomain());
    }
    json.put("enabled", obj.isEnabled());
    json.put("step", obj.getStep());
  }
}