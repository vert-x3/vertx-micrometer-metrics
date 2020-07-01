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
package io.vertx.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Joel Takvorian
 */
public final class RegistryInspector {
  public static Predicate<Meter> ALL = a -> true;

  private RegistryInspector() {
  }

  public static void dump(Predicate<Meter> predicate) {
    dump(MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME, predicate);
  }

  public static void dump(String regName, Predicate<Meter> predicate) {
    listDatapoints(regName, predicate).forEach(System.out::println);
  }

  public static Predicate<Meter> startsWith(String start) {
    return m -> m.getId().getName().startsWith(start);
  }

  public static List<Datapoint> listDatapoints(Predicate<Meter> predicate) {
    return listDatapoints(MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME, predicate);
  }

  public static List<Datapoint> listDatapoints(String regName, Predicate<Meter> predicate) {
    List<Datapoint> result = new ArrayList<>();
    MeterRegistry registry = BackendRegistries.getNow(regName);
    if (registry == null) {
      throw new NoRegistryException(regName);
    }
    registry.forEachMeter(m -> {
      if (predicate.test(m)) {
        String id = id(m);
        m.measure().forEach(measurement -> {
          result.add(new Datapoint(id + "$" + measurement.getStatistic().name(), measurement.getValue()));
        });
      }
    });
    return result;
  }

  private static String id(Meter m) {
    return m.getId().getName() + "["
      + StreamSupport.stream(m.getId().getTags().spliterator(), false)
          .map(t -> t.getKey() + '=' + t.getValue())
          .collect(Collectors.joining(","))
      + "]";
  }

  public static RegistryInspector.Datapoint dp(String id, double value) {
    return new Datapoint(id, value);
  }

  public static RegistryInspector.Datapoint dp(String id, int value) {
    return new Datapoint(id, (double) value);
  }

  public static void waitForValue(Vertx vertx, TestContext context, String fullName, Predicate<Double> p) {
    waitForValue(vertx, context, MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME, fullName, p);
  }

  public static void waitForValue(Vertx vertx, TestContext context, String regName, String fullName, Predicate<Double> p) {
    Async ready = context.async();
    vertx.setPeriodic(200, id -> {
      try {
        RegistryInspector.listDatapoints(regName, m -> true).stream()
          .filter(dp -> fullName.equals(dp.id()))
          .filter(dp -> p.test(dp.value()))
          .findAny()
          .ifPresent(dp -> {
            vertx.cancelTimer(id);
            ready.complete();
          });
      } catch (NoRegistryException e) {
        context.fail(e);
        vertx.cancelTimer(id);
      }
    });
    ready.awaitSuccess(10000);
  }

  public static class Datapoint {
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

  public static class NoRegistryException extends RuntimeException {
    public NoRegistryException(String regName) {
      super("Registry '" + regName + "' not found");
    }
  }
}
