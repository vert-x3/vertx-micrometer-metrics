/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.monitoring.influxdb;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;

/**
 * Vert.x InfluxDb monitoring configuration.
 *
 * @author Dan Kristensen
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxInfluxDbOptions extends BatchingReporterOptions {
  /**
   * The default InfluxDb server host = localhost.
   */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * The default InfluxDb server port = 8086.
   */
  public static final int DEFAULT_PORT = 8086;

  /**
   * The default InfluxDb Metrics service URI = /write.
   */
  public static final String DEFAULT_METRICS_URI = "/write";

  /**
   * The default InfluxDb database = default.
   */
  public static final String DEFAULT_DATABASE = "default";

  /**
   * The default prefix = vert.x.
   */
  public static final String DEFAULT_PREFIX = "vert.x";

  /**
   * The default gzip enabled on InfluxDb.
   */
  public static final boolean DEFAULT_GZIP_ENABLED = true;

  private String host;
  private int port;
  private HttpClientOptions httpOptions;
  private String metricsServiceUri;
  private String database;
  private AuthenticationOptions authenticationOptions;
  private JsonObject httpHeaders;

  private boolean gzipEnabled;

  public VertxInfluxDbOptions() {
    host = DEFAULT_HOST;
    port = DEFAULT_PORT;
    gzipEnabled = DEFAULT_GZIP_ENABLED;
    httpOptions = new HttpClientOptions();
    metricsServiceUri = DEFAULT_METRICS_URI;
    database = DEFAULT_DATABASE;
    setPrefix(DEFAULT_PREFIX);
    authenticationOptions = new AuthenticationOptions();
    httpHeaders = new JsonObject();
  }

  public VertxInfluxDbOptions(VertxInfluxDbOptions other) {
    super(other);
    host = other.host;
    port = other.port;
    httpOptions = other.httpOptions != null ? new HttpClientOptions(other.httpOptions) : new HttpClientOptions();
    metricsServiceUri = other.metricsServiceUri;
    database = other.database;
    authenticationOptions = other.authenticationOptions != null ? new AuthenticationOptions(other.authenticationOptions) : new AuthenticationOptions();
    httpHeaders = other.httpHeaders;
  }

  public VertxInfluxDbOptions(JsonObject json) {
    this();
    VertxInfluxDbOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the InfluxDb Metrics service host
   */
  public String getHost() {
    return host;
  }

  /**
   * Set the InfluxDb Metrics service host. Defaults to {@code localhost}.
   */
  public VertxInfluxDbOptions setHost(String host) {
    this.host = host;
    return this;
  }

  /**
   * @return the InfluxDb Metrics service port.
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the InfluxDb Metrics service port.  Defaults to {@code 8080}.
   */
  public VertxInfluxDbOptions setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return the configuration of the InfluxDb Metrics HTTP client
   */
  public HttpClientOptions getHttpOptions() {
    return httpOptions;
  }

  /**
   * Set the configuration of the InfluxDb Metrics HTTP client.
   */
  public VertxInfluxDbOptions setHttpOptions(HttpClientOptions httpOptions) {
    this.httpOptions = httpOptions;
    return this;
  }

  /**
   * @return the InfluxDb Metrics service URI
   */
  public String getMetricsServiceUri() {
    return metricsServiceUri;
  }

  /**
   * Set the InfluxDb Metrics service URI. Defaults to {@code /InfluxDb/metrics}. This can be useful if you host the
   * InfluxDb server behind a proxy and manipulate the default service URI.
   */
  public VertxInfluxDbOptions setMetricsServiceUri(String metricsServiceUri) {
    this.metricsServiceUri = metricsServiceUri;
    return this;
  }

  /**
   * @return the InfluxDb tenant
   */
  public String getDatabase() {
    return database;
  }

  /**
   * Set the InfluxDb database. Defaults to {@code default}.
   */
  public VertxInfluxDbOptions setDatabase(String database) {
    this.database = database;
    return this;
  }

  /**
   * @return the authentication options
   */
  public AuthenticationOptions getAuthenticationOptions() {
    return authenticationOptions;
  }

  /**
   * Set the options for authentication.
   */
  public VertxInfluxDbOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
    this.authenticationOptions = authenticationOptions;
    return this;
  }

  /**
   * @return specific headers to include in HTTP requests
   */
  public JsonObject getHttpHeaders() {
    return httpHeaders;
  }

  /**
   * Set specific headers to include in HTTP requests.
   */
  public VertxInfluxDbOptions setHttpHeaders(JsonObject httpHeaders) {
    this.httpHeaders = httpHeaders;
    return this;
  }

  public boolean isGzipEnabled() {
    return gzipEnabled;
  }

  public VertxInfluxDbOptions setGzipEnabled(boolean gzipEnabled) {
    this.gzipEnabled = gzipEnabled;
    return this;
  }
}
