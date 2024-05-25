/*
 * Copyright 2018 Red Hat, Inc.
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

package io.vertx.micrometer.impl;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;

/**
 * Handler to expose Prometheus metrics via an HTTP endpoint.
 * <p>
 * This handler can be used to scrape metrics from a PrometheusMeterRegistry and serve them over HTTP.
 * </p>
 *
 * @author Swamy Mavuri
 */
public class PrometheusRequestHandlerImpl implements Handler<HttpServerRequest> {

  private final PrometheusMeterRegistry registry;
  private final String metricsEndpoint;

  /**
   * Constructs a handler with the specified registry and metrics endpoint.
   *
   * @param registry        the PrometheusMeterRegistry to use for scraping metrics
   * @param metricsEndpoint the endpoint to expose metrics
   */
  public PrometheusRequestHandlerImpl(PrometheusMeterRegistry registry, String metricsEndpoint) {
    this.registry = registry;
    this.metricsEndpoint = metricsEndpoint;
  }

  /**
   * Constructs a handler with the specified registry and the default metrics endpoint ("/metrics").
   *
   * @param registry the PrometheusMeterRegistry to use for scraping metrics
   */
  public PrometheusRequestHandlerImpl(PrometheusMeterRegistry registry) {
    this.registry = registry;
    this.metricsEndpoint = "/metrics";
  }

  /**
   * Constructs a handler with a new PrometheusMeterRegistry and the default metrics endpoint ("/metrics").
   */
  public PrometheusRequestHandlerImpl() {
    this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    this.metricsEndpoint = "/metrics";
  }

  @Override
  public void handle(HttpServerRequest request) {
    if (metricsEndpoint.equals(request.path())) {
      request.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain; version=0.0.4; charset=utf-8")
        .end(registry.scrape());
    } else {
      request.response().setStatusCode(404).end();
    }
  }
}
