/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MicrometerMetricsTestBase {

  public static Predicate<Meter> ALL = a -> true;

  protected String registryName;
  protected MicrometerMetricsOptions metricsOptions;
  protected Vertx vertx;

  @Before
  public void before(TestContext context) {
    setUp(context);
  }

  protected void setUp(TestContext context) {
    registryName = UUID.randomUUID().toString();
    metricsOptions = metricOptions();
  }

  protected MicrometerMetricsOptions metricOptions() {
    return new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setRegistryName(registryName)
      .setEnabled(true);
  }

  protected Vertx vertx(TestContext context) {
    return Vertx.vertx(new VertxOptions().setMetricsOptions(metricsOptions))
      .exceptionHandler(context.exceptionHandler());
  }

  @After
  public void after(TestContext context) {
    tearDown(context);
  }

  protected void tearDown(TestContext context) {
    if (vertx != null) {
      vertx.close(context.asyncAssertSuccess());
    }
  }

  public void waitForValue(TestContext context, String fullName, Predicate<Double> p) {
    Async ready = context.async();
    vertx.setPeriodic(200, id -> {
      try {
        listDatapoints(m -> true).stream()
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

  public List<Datapoint> listDatapoints(Predicate<Meter> predicate) {
    List<Datapoint> result = new ArrayList<>();
    MeterRegistry registry = BackendRegistries.getNow(registryName);
    if (registry == null) {
      throw new NoRegistryException(registryName);
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
      + m.getId().getTags().stream()
      .map(t -> t.getKey() + '=' + t.getValue())
      .collect(Collectors.joining(","))
      + "]";
  }

  public static Predicate<Meter> startsWith(String start) {
    return m -> m.getId().getName().startsWith(start);
  }

  public static Datapoint dp(String id, double value) {
    return new Datapoint(id, value);
  }

  public static Datapoint dp(String id, int value) {
    return new Datapoint(id, (double) value);
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
