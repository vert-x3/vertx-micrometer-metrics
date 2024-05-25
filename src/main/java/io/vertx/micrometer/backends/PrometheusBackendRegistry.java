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
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.micrometer.PrometheusRequestHandler;
import io.vertx.micrometer.VertxPrometheusOptions;

/**
 * @author Joel Takvorian
 */
public final class PrometheusBackendRegistry implements BackendRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusBackendRegistry.class);

  private final PrometheusMeterRegistry registry;
  private final VertxPrometheusOptions options;
  private Vertx vertx;

  public PrometheusBackendRegistry(VertxPrometheusOptions options) {
    this(options, new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
  }

  public PrometheusBackendRegistry(VertxPrometheusOptions options, PrometheusMeterRegistry registry) {
    this.options = options;
    this.registry = registry;
    if (options.isPublishQuantiles()) {
      registry.config().meterFilter(
        new MeterFilter() {
          @Override
          public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
            return DistributionStatisticConfig.builder()
              .percentilesHistogram(true)
              .build()
              .merge(config);
          }
        });
    }
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return registry;
  }

  @Override
  public void init() {
    if (options.isStartEmbeddedServer()) {
      this.vertx = Vertx.vertx();
      // Start dedicated server
      HttpServerOptions serverOptions = options.getEmbeddedServerOptions();
      if (serverOptions == null) {
        serverOptions = new HttpServerOptions();
      }
      vertx.createHttpServer(serverOptions)
        .requestHandler(PrometheusRequestHandler.create(registry, options.getEmbeddedServerEndpoint()))
        .exceptionHandler(t -> LOGGER.error("Error in Prometheus registry embedded server", t))
        .listen(serverOptions.getPort(), serverOptions.getHost());
    }
  }

  @Override
  public void close() {
    if (this.vertx != null) {
      vertx.close();
    }
  }
}
