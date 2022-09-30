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
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;

import static io.vertx.codegen.annotations.GenIgnore.*;

/**
 * A Vert.x Web {@link io.vertx.ext.web.Route} handler for Prometheus metrics scraping.
 *
 * @author Thomas Segismont
 */
@VertxGen
public interface PrometheusScrapingHandler {

  /**
   * Creates a Vert.x Web {@link io.vertx.ext.web.Route} handler for Prometheus metrics scraping.
   * The default backend registry is used.
   *
   * @return a {@link io.vertx.ext.web.Route} handler for the default backend registry
   * @see BackendRegistries#getDefaultNow()
   */
  static Handler<RoutingContext> create() {
    return new PrometheusScrapingHandlerImpl();
  }

  /**
   * Creates a Vert.x Web {@link io.vertx.ext.web.Route} handler for Prometheus metrics scraping.
   * The registry specified by {@code registryName} is used.
   *
   * @param registryName the backend metrics registry
   * @return a {@link io.vertx.ext.web.Route} handler for a specific metrics registry
   * @see BackendRegistries#getNow(String)
   */
  static Handler<RoutingContext> create(String registryName) {
    return new PrometheusScrapingHandlerImpl(registryName);
  }

  /**
   * Creates a Vert.x Web {@link io.vertx.ext.web.Route} handler for Prometheus metrics scraping.
   * The registry specified by {@code registry} is used.
   *
   * @param registry the backend metrics registry
   * @return a {@link io.vertx.ext.web.Route} handler for a specific metrics registry
   */
  @GenIgnore(PERMITTED_TYPE)
  static Handler<RoutingContext> create(PrometheusMeterRegistry registry) {
    return new PrometheusScrapingHandlerImpl(registry);
  }
}
