/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Meter;
import io.netty.util.concurrent.FastThreadLocal;

import java.util.HashMap;
import java.util.Map;

class MeterCache {

  private final FastThreadLocal<Map<Meter.Id, Object>> threadLocal;

  MeterCache() {
    threadLocal = new FastThreadLocal<Map<Meter.Id, Object>>() {
      @Override
      protected Map<Meter.Id, Object> initialValue() {
        return new HashMap<>();
      }

      @Override
      protected void onRemoval(Map<Meter.Id, Object> value) {
        value.clear();
      }
    };
  }

  @SuppressWarnings("unchecked")
  <T> T get(Meter.Id id) {
    return (T) threadLocal.get().get(id);
  }

  void put(Meter.Id id, Object value) {
    threadLocal.get().put(id, value);
  }

  void close() {
    threadLocal.remove();
  }
}
