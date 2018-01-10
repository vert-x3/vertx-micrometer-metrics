/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package io.vertx.monitoring;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.monitoring.backend.BackendRegistries;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * @author Joel Takvorian
 */
final class RegistryInspector {

  private RegistryInspector() {
  }

  static List<Datapoint> listWithoutTimers(String startsWith) {
    return listWithoutTimers(startsWith, VertxMonitoringOptions.DEFAULT_REGISTRY_NAME);
  }

  static List<Datapoint> listTimers(String startsWith) {
    return listTimers(startsWith, VertxMonitoringOptions.DEFAULT_REGISTRY_NAME);
  }

  static List<Datapoint> listWithoutTimers(String startsWith, String regName) {
    MeterRegistry registry = BackendRegistries.getNow(regName);
    return registry.getMeters().stream()
      .filter(m -> m.type() != Meter.Type.Timer && m.type() != Meter.Type.LongTaskTimer)
      .filter(m -> m.getId().getName().startsWith(startsWith))
      .flatMap(m -> {
        String id = id(m);
        return StreamSupport.stream(m.measure().spliterator(), false)
          .map(measurement -> new Datapoint(id + "$" + measurement.getStatistic().name(), measurement.getValue()));
      })
      .collect(toList());
  }

  static List<Datapoint> listTimers(String startsWith, String regName) {
    MeterRegistry registry = BackendRegistries.getNow(regName);
    return registry.getMeters().stream()
      .filter(m -> m.type() == Meter.Type.Timer || m.type() == Meter.Type.LongTaskTimer)
      .filter(m -> m.getId().getName().startsWith(startsWith))
      .flatMap(m -> {
        String id = id(m);
        return StreamSupport.stream(m.measure().spliterator(), false)
          .map(measurement -> new Datapoint(id + "$" + measurement.getStatistic().name(), measurement.getValue()));
      })
      .collect(toList());
  }

  private static String id(Meter m) {
    return m.getId().getName() + "["
      + StreamSupport.stream(m.getId().getTags().spliterator(), false)
          .map(t -> t.getKey() + '=' + t.getValue())
          .collect(Collectors.joining(","))
      + "]";
  }

  static RegistryInspector.Datapoint dp(String id, double value) {
    return new Datapoint(id, value);
  }

  static RegistryInspector.Datapoint dp(String id, int value) {
    return new Datapoint(id, (double) value);
  }

  static void waitUntil(String id, int value) {

  }

  static class Datapoint {
    private final String id;
    private final Double value;

    private Datapoint(String id, Double value) {
      this.id = id;
      this.value = value;
    }

    String id() {
      return id;
    }

    Double value() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Datapoint datapoint = (Datapoint) o;
      return Objects.equals(id, datapoint.id) &&
        Objects.equals(value, datapoint.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, value);
    }

    @Override
    public String toString() {
      return id + "/" + value;
    }
  }
}
