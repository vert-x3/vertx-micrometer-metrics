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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.VertxPrometheusOptions;

/**
 * @author Joel Takvorian
 */
public final class PrometheusBackendRegistry implements BackendRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusBackendRegistry.class);

  private final PrometheusMeterRegistry registry;
  private final Vertx vertx;
  private final VertxPrometheusOptions options;

  public PrometheusBackendRegistry(VertxPrometheusOptions options) {
    this.vertx = Vertx.vertx();
    this.options = options;
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return registry;
  }

  @Override
  public void init() {
    if (options.isStartEmbeddedServer()) {
      // Start dedicated server
      HttpServerOptions serverOptions = options.getEmbeddedServerOptions();
      if (serverOptions == null) {
        serverOptions = new HttpServerOptions();
      }
      Router router = Router.router(vertx);
      router.route(options.getEmbeddedServerEndpoint()).handler(routingContext -> {
        String response = registry.scrape();
        routingContext.response().end(response);
      });
      vertx.createHttpServer(serverOptions)
        .requestHandler(router)
        .exceptionHandler(t -> LOGGER.error("Error in Prometheus registry embedded server", t))
        .listen(serverOptions.getPort(), serverOptions.getHost());
    }
  }

  @Override
  public void close() {
    vertx.close();
  }
}
