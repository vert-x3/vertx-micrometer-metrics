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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.Objects;
import static io.vertx.core.http.HttpHeaders.*;

/**
 * @author Thomas Segismont
 */
public class PrometheusScrapingHandlerImpl implements Handler<RoutingContext> {

  private final String registryName;
  private final PrometheusMeterRegistry registry;

  public PrometheusScrapingHandlerImpl() {
    registryName = null;
    registry = null;
  }

  public PrometheusScrapingHandlerImpl(String registryName) {
    this.registryName = Objects.requireNonNull(registryName);
    registry = null;
  }

  public PrometheusScrapingHandlerImpl(PrometheusMeterRegistry registry) {
    registryName = null;
    this.registry = Objects.requireNonNull(registry);
  }

  @Override
  public void handle(RoutingContext rc) {
    if (registry != null) {
      scrapeAndReply(rc, registry);
      return;
    }
    MeterRegistry registry;
    if (registryName == null) {
      registry = BackendRegistries.getDefaultNow();
    } else {
      registry = BackendRegistries.getNow(registryName);
    }
    if (registry instanceof PrometheusMeterRegistry) {
      scrapeAndReply(rc, (PrometheusMeterRegistry) registry);
    } else {
      String statusMessage = "Invalid registry: " + (registry != null ? registry.getClass().getName() : null);
      rc.response()
        .setStatusCode(500).setStatusMessage(statusMessage)
        .end();
    }
  }

  private static void scrapeAndReply(RoutingContext rc, PrometheusMeterRegistry prometheusMeterRegistry) {
    rc.response()
      .putHeader(CONTENT_TYPE, "text/plain; version=0.0.4; charset=utf-8")
      .end(prometheusMeterRegistry.scrape());
  }
}
