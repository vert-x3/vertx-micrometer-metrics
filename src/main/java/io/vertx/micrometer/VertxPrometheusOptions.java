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
package io.vertx.micrometer;

import io.prometheus.client.CollectorRegistry;
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
   * Default value for starting an embedded server = false.
   */
  public static final boolean DEFAULT_START_EMBEDDED_SERVER = false;

  /**
   * The default metrics endpoint = /metrics when using an embedded server.
   */
  public static final String DEFAULT_EMBEDDED_SERVER_ENDPOINT = "/metrics";

  /**
   * Default value for publishing histogram quantiles = false.
   */
  public static final boolean DEFAULT_PUBLISH_QUANTILES = false;

  private boolean enabled;
  private boolean startEmbeddedServer;
  private HttpServerOptions embeddedServerOptions;
  private String embeddedServerEndpoint;
  private boolean publishQuantiles;
  private CollectorRegistry collectorRegistry;

  /**
   * Default constructor
   */
  public VertxPrometheusOptions() {
    enabled = DEFAULT_ENABLED;
    startEmbeddedServer = DEFAULT_START_EMBEDDED_SERVER;
    embeddedServerEndpoint = DEFAULT_EMBEDDED_SERVER_ENDPOINT;
    publishQuantiles = DEFAULT_PUBLISH_QUANTILES;
  }

  /**
   * Copy constructor
   *
   * @param other The other {@link VertxPrometheusOptions} to copy when creating this
   */
  public VertxPrometheusOptions(VertxPrometheusOptions other) {
    enabled = other.enabled;
    startEmbeddedServer = other.startEmbeddedServer;
    embeddedServerEndpoint = other.embeddedServerEndpoint != null ? other.embeddedServerEndpoint : DEFAULT_EMBEDDED_SERVER_ENDPOINT;
    if (other.embeddedServerOptions != null) {
      embeddedServerOptions = new HttpServerOptions(other.embeddedServerOptions);
    }
    publishQuantiles = other.publishQuantiles;
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


  /**
   * @return a JSON representation of these options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    VertxPrometheusOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Will Prometheus reporting be enabled?
   *
   * @return true if enabled, false if not.
   */
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

  /**
   * Returns true if it is configured to init an embedded web server to expose Prometheus metrics
   */
  public boolean isStartEmbeddedServer() {
    return startEmbeddedServer;
  }

  /**
   * When true, an embedded server will init to expose metrics with Prometheus format.
   */
  public VertxPrometheusOptions setStartEmbeddedServer(boolean startEmbeddedServer) {
    this.startEmbeddedServer = startEmbeddedServer;
    return this;
  }

  /**
   * Get the HTTP server options of the embedded server, if any
   */
  public HttpServerOptions getEmbeddedServerOptions() {
    return embeddedServerOptions;
  }

  /**
   * HTTP server options for the embedded server
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

  /**
   * Get the HTTP endpoint used if an embedded server is configured
   */
  public String getEmbeddedServerEndpoint() {
    return embeddedServerEndpoint;
  }

  /**
   * @return true if quantile stats are published
   */
  public boolean isPublishQuantiles() {
    return publishQuantiles;
  }

  /**
   * Set true to publish histogram stats, necessary to compute quantiles.
   * Note that it generates many new timeseries for stats, which is why it is deactivated by default.
   *
   * @param publishQuantiles the publishing quantiles flag
   * @return a reference to this, so the API can be used fluently
   */
  public VertxPrometheusOptions setPublishQuantiles(boolean publishQuantiles) {
    this.publishQuantiles = publishQuantiles;
    return this;
  }

  /**
   * Set collector registry. The new one will be created if not set.
   * @param collectorRegistry the registry
   */
  public VertxPrometheusOptions setCollectorRegistry(CollectorRegistry collectorRegistry) {
    this.collectorRegistry = collectorRegistry;
    return this;
  }

  /**
   * Get the collector registry if configured.
   */
  public CollectorRegistry getCollectorRegistry() {
    return collectorRegistry;
  }
}
