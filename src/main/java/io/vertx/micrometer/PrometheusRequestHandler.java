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

package io.vertx.micrometer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.micrometer.impl.PrometheusRequestHandlerImpl;

/**
 * An interface for creating handlers to expose Prometheus metrics via an HTTP endpoint.
 * <p>
 * This interface provides factory methods to create handlers that can scrape metrics from a
 * PrometheusMeterRegistry and serve them over HTTP. It allows for various configurations of
 * the metrics endpoint and the Prometheus registry.
 * </p>
 *
 * @see PrometheusMeterRegistry
 * @see Handler
 * @see HttpServerRequest
 *
 * @author Swamy Mavuri
 */
@VertxGen
public interface PrometheusRequestHandler {

  /**
   * Creates a handler with the specified PrometheusMeterRegistry and metrics endpoint.
   * <p>
   * This handler scrapes metrics from the given PrometheusMeterRegistry and serves them
   * at the specified endpoint.
   * </p>
   *
   * @param registry       the PrometheusMeterRegistry to use for scraping metrics
   * @param metricsEndpoint the endpoint to expose metrics
   * @return a handler for scraping Prometheus metrics
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static Handler<HttpServerRequest> create(PrometheusMeterRegistry registry, String metricsEndpoint) {
    return new PrometheusRequestHandlerImpl(registry, metricsEndpoint);
  }

  /**
   * Creates a handler with the specified PrometheusMeterRegistry and the default metrics endpoint ("/metrics").
   * <p>
   * This handler scrapes metrics from the given PrometheusMeterRegistry and serves them
   * at the default endpoint "/metrics".
   * </p>
   *
   * @param registry the PrometheusMeterRegistry to use for scraping metrics
   * @return a handler for scraping Prometheus metrics
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static Handler<HttpServerRequest> create(PrometheusMeterRegistry registry) {
    return new PrometheusRequestHandlerImpl(registry);
  }

  /**
   * Creates a handler with a new PrometheusMeterRegistry and the default metrics endpoint ("/metrics").
   * <p>
   * This handler scrapes metrics from a newly created PrometheusMeterRegistry and serves them
   * at the default endpoint "/metrics".
   * </p>
   *
   * @return a handler for scraping Prometheus metrics
   */
  static Handler<HttpServerRequest> create() {
    return new PrometheusRequestHandlerImpl();
  }
}
