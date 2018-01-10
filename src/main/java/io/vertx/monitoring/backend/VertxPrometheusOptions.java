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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;

/**
 * Options for Prometheus metrics backend.
 *
 * @author Joel Takvorian
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxPrometheusOptions {

  /**
   * Default value for enabled = false.
   */
  public static final boolean DEFAULT_ENABLED = false;

  /**
   * The default metrics endpoint = /metrics when using an embedded server.
   */
  public static final String DEFAULT_EMBEDDED_SERVER_ENDPOINT = "/metrics";

  private boolean enabled;
  private HttpServerOptions embeddedServerOptions;
  private String embeddedServerEndpoint;

  /**
   * Default constructor
   */
  public VertxPrometheusOptions() {
    enabled = DEFAULT_ENABLED;
    embeddedServerEndpoint = DEFAULT_EMBEDDED_SERVER_ENDPOINT;
  }

  /**
   * Copy constructor
   *
   * @param other The other {@link VertxPrometheusOptions} to copy when creating this
   */
  public VertxPrometheusOptions(VertxPrometheusOptions other) {
    enabled = other.enabled;
    embeddedServerEndpoint = other.embeddedServerEndpoint != null ? other.embeddedServerEndpoint : DEFAULT_EMBEDDED_SERVER_ENDPOINT;
    if (other.embeddedServerOptions != null) {
      embeddedServerOptions = new HttpServerOptions(other.embeddedServerOptions);
    }
  }

  /**
   * Create an instance from a {@link io.vertx.core.json.JsonObject}
   *
   * @param json the JsonObject to create it from
   */
  public VertxPrometheusOptions(JsonObject json) {
    this();
    VertxPrometheusOptionsConverter.fromJson(json, this);
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set true to enable Prometheus reporting
   */
  public VertxPrometheusOptions setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public HttpServerOptions getEmbeddedServerOptions() {
    return embeddedServerOptions;
  }

  /**
   * When set, an embedded server will start to expose metrics with Prometheus format
   * @param embeddedServerOptions the server options
   */
  public VertxPrometheusOptions setEmbeddedServerOptions(HttpServerOptions embeddedServerOptions) {
    this.embeddedServerOptions = embeddedServerOptions;
    return this;
  }

  /**
   * Set metrics endpoint. Use conjointly with the embedded server options. Defaults to <i>/metrics</i>.
   * @param embeddedServerEndpoint metrics endpoint
   */
  public VertxPrometheusOptions setEmbeddedServerEndpoint(String embeddedServerEndpoint) {
    this.embeddedServerEndpoint = embeddedServerEndpoint;
    return this;
  }

  public String getEmbeddedServerEndpoint() {
    return embeddedServerEndpoint;
  }
}
