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
package io.vertx.micrometer.backends;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxInfluxDbOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.*;

/**
 * {@link BackendRegistries} is responsible for managing registries related to particular micrometer backends (influxdb, prometheus...)
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
   * @param options micrometer options, including configuration related to the backend.
   *                Should be a subclass of {@link MicrometerMetricsOptions} (ex: {@link VertxInfluxDbOptions}, {@link VertxPrometheusOptions}).
   *                If the class is not recognized, a {@link NoopBackendRegistry} will be returned.
   * @return the created (or existing) {@link BackendRegistry}
   */
  public static BackendRegistry setupBackend(MicrometerMetricsOptions options, MeterRegistry meterRegistry) {
    return REGISTRIES.computeIfAbsent(options.getRegistryName(), k -> {
      final BackendRegistry reg;
      if (meterRegistry != null) {
        if (options.getPrometheusOptions() != null && meterRegistry instanceof PrometheusMeterRegistry) {
          // If a Prometheus registry is provided, extra initialization steps may have to be performed
          reg = new PrometheusBackendRegistry(options.getPrometheusOptions(), (PrometheusMeterRegistry) meterRegistry);
        } else {
          // Other backend registries have no special extra steps
          reg = () -> meterRegistry;
        }
      } else if (options.getInfluxDbOptions() != null && options.getInfluxDbOptions().isEnabled()) {
        reg = new InfluxDbBackendRegistry(options.getInfluxDbOptions());
      } else if (options.getPrometheusOptions() != null && options.getPrometheusOptions().isEnabled()) {
        reg = new PrometheusBackendRegistry(options.getPrometheusOptions());
      } else if (options.getJmxMetricsOptions() != null && options.getJmxMetricsOptions().isEnabled()) {
        reg = new JmxBackendRegistry(options.getJmxMetricsOptions());
      } else {
        // No backend setup, use global registry
        reg = NoopBackendRegistry.INSTANCE;
      }
      registerMatchers(reg.getMeterRegistry(), options.getLabels(), options.getLabelMatches());
      return reg;
    });
  }

  /**
   * Get the default micrometer registry.
   * May return {@code null} if it hasn't been registered yet or if it has been stopped.
   * @return the micrometer registry or {@code null}
   */
  public static MeterRegistry getDefaultNow() {
    return getNow(MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME);
  }

  /**
   * Get the micrometer registry of the given name.
   * May return {@code null} if it hasn't been registered yet or if it has been stopped.
   * @param registryName the name associated with this registry in Micrometer options
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
   * @param registryName the name associated with this registry in Micrometer options
   */
  public static void stop(String registryName) {
    BackendRegistry reg = REGISTRIES.remove(registryName);
    if (reg != null) {
      reg.close();
    }
  }

  public static void registerMatchers(MeterRegistry registry, Set<Label> enabledLabels, List<Match> matches) {
    Set<String> ignored = EnumSet.complementOf(EnumSet.copyOf(enabledLabels)).stream()
      .map(Label::toString)
      .collect(toSet());
    if (!ignored.isEmpty()) {
      registry.config().meterFilter(ignoreTags(ignored));
    }
    matches.forEach(m -> {
      switch (m.getType()) {
        case EQUALS:
          if (m.getAlias() == null) {
            // Exact match => accept
            registry.config().meterFilter(MeterFilter.deny(id -> {
              if (m.getDomain() != null && !id.getName().startsWith(m.getDomain().getPrefix())) {
                // If domain has been specified and we're not in that domain, ignore rule
                return false;
              }
              String tagValue = id.getTag(m.getLabel());
              return !m.getValue().equals(tagValue);
            }));
          } else {
            // Exact match => alias
            registry.config().meterFilter(replaceTagValues(
              m.getDomain(),
              m.getLabel(),
              val -> {
                if (m.getValue().equals(val)) {
                  return m.getAlias();
                }
                return val;
              }
            ));
          }
          break;
        case REGEX:
          Pattern pattern = Pattern.compile(m.getValue());
          if (m.getAlias() == null) {
            // Regex match => accept
            registry.config().meterFilter(MeterFilter.accept(id -> {
              if (m.getDomain() != null && !id.getName().startsWith(m.getDomain().getPrefix())) {
                // If domain has been specified and we're not in that domain, ignore rule
                return true;
              }
              String tagValue = id.getTag(m.getLabel());
              if (tagValue == null) {
                return false;
              }
              return pattern.matcher(tagValue).matches();
            }));
          } else {
            // Regex match => alias
            registry.config().meterFilter(replaceTagValues(
              m.getDomain(),
              m.getLabel(),
              val -> {
                if (pattern.matcher(val).matches()) {
                  return m.getAlias();
                }
                return val;
              }
            ));
          }
          break;
      }
    });
  }

  private static MeterFilter ignoreTags(Set<String> ignored) {
    return new MeterFilter() {
      @Override
      public Meter.Id map(Meter.Id id) {
        List<Tag> tags = new ArrayList<>();
        int count = 0;
        for (Tag tag : id.getTagsAsIterable()) {
          if (!ignored.contains(tag.getKey())) {
            tags.add(tag);
          }
          count++;
        }
        return tags.size() == count ? id : id.replaceTags(tags);
      }
    };
  }

  private static MeterFilter replaceTagValues(MetricsDomain domain, String tagKey, Function<String, String> replacement) {
    return new MeterFilter() {
      @Override
      public Meter.Id map(Meter.Id id) {
        if (domain != null && !id.getName().startsWith(domain.getPrefix())) {
          return id;
        }
        return MeterFilter.replaceTagValues(tagKey, replacement).map(id);
      }
    };
  }
}
