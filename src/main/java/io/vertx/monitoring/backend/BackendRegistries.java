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
package io.vertx.monitoring.backend;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.monitoring.VertxMonitoringOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link BackendRegistries} is responsible for managing registries related to particular monitoring backends (influxdb, prometheus...)
 * It contains a store of {@link BackendRegistry} objects, each of whose encapsulating a micrometer's {@link MeterRegistry}
 * @author Joel Takvorian
 */
public final class BackendRegistries {
  private static final Map<String, BackendRegistry> REGISTRIES = new ConcurrentHashMap<>();

  private BackendRegistries() {
  }

  /**
   * Create a new backend registry, containing a micrometer registry, initialized with the provided options.
   * If a registry already exists with the associated name, it is just returned without any effect.
   * @param vertx vertx object that might be necessary for some backend initialization
   * @param options monitoring options, including configuration related to the backend.
   *                Should be a subclass of {@link VertxMonitoringOptions} (ex: {@link VertxInfluxDbOptions}, {@link VertxPrometheusOptions}).
   *                If the class is not recognized, a {@link NoopBackendRegistry} will be returned.
   * @return the created (or existing) {@link BackendRegistry}
   */
  public static BackendRegistry setupBackend(Vertx vertx, VertxMonitoringOptions options) {
    return REGISTRIES.computeIfAbsent(options.getRegistryName(), k -> {
      final BackendRegistry reg;
      if (options.getInfluxDbOptions() != null && options.getInfluxDbOptions().isEnabled()) {
        reg = new InfluxDbBackendRegistry(options.getInfluxDbOptions());
      } else if (options.getPrometheusOptions() != null && options.getPrometheusOptions().isEnabled()) {
        reg = new PrometheusBackendRegistry(vertx, options.getPrometheusOptions());
      } else {
        // No backend setup, use global registry
        reg = NoopBackendRegistry.INSTANCE;
      }
      return reg;
    });
  }

  /**
   * Get the default micrometer registry.
   * May return {@code null} if it hasn't been registered yet or if it has been stopped.
   * @return the micrometer registry or {@code null}
   */
  public static MeterRegistry getDefaultNow() {
    return getNow(VertxMonitoringOptions.DEFAULT_REGISTRY_NAME);
  }

  /**
   * Get the micrometer registry of the given name.
   * May return {@code null} if it hasn't been registered yet or if it has been stopped.
   * @param registryName the name associated with this registry in Vert.x Monitoring options
   * @return the micrometer registry or {@code null}
   */
  public static MeterRegistry getNow(String registryName) {
    BackendRegistry backendRegistry = REGISTRIES.get(registryName);
    if (backendRegistry != null) {
      return backendRegistry.getMeterRegistry();
    }
    return null;
  }

  /**
   * Stop (unregister) the backend registry of the given name.
   * Any resource started by this backend registry will be released (like running HTTP server)
   * @param registryName the name associated with this registry in Vert.x Monitoring options
   */
  public static void stop(String registryName) {
    BackendRegistry reg = REGISTRIES.remove(registryName);
    if (reg != null) {
      reg.close();
    }
  }
}
