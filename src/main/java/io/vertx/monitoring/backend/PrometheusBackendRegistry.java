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
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

/**
 * @author Joel Takvorian
 */
public final class PrometheusBackendRegistry implements BackendRegistry {
  private final PrometheusMeterRegistry registry;
  private final Vertx vertx;
  private final VertxPrometheusOptions options;
  private HttpServer server;

  public PrometheusBackendRegistry(Vertx vertx, VertxPrometheusOptions options) {
    this.vertx = vertx;
    this.options = options;
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return registry;
  }

  @Override
  public void eventBusInitialized(EventBus bus) {
    if (options.getEmbeddedServerOptions() != null) {
      // Start dedicated server
      HttpServerOptions serverOptions = options.getEmbeddedServerOptions();
      Router router = Router.router(vertx);
      router.route(options.getEmbeddedServerEndpoint()).handler(routingContext -> {
        String response = registry.scrape();
        routingContext.response().end(response);
      });
      server = vertx.createHttpServer(serverOptions)
        .requestHandler(router::accept)
        .listen(serverOptions.getPort(), serverOptions.getHost());
    }
  }

  @Override
  public void close() {
    // registry.clear();
    if (server != null) {
      server.close();
    }
  }
}
