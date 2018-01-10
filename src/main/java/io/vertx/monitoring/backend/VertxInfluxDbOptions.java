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
package io.vertx.monitoring.backend;

import io.micrometer.influx.InfluxConfig;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.time.Duration;

/**
 * Vert.x InfluxDb monitoring configuration.
 *
 * @author Dan Kristensen
 * @author Joel Takvorian
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxInfluxDbOptions implements InfluxConfig {

  /**
   * Default value for enabled = false.
   */
  public static final boolean DEFAULT_ENABLED = false;

  /**
   * Default value for metric collection interval (in seconds) = 10.
   */
  public static final int DEFAULT_STEP = 10;

  /**
   * Default value for the maximum number of metrics in a batch = 10000.
   */
  public static final int DEFAULT_BATCH_SIZE = 10000;

  /**
   * The default InfluxDb server URI = http://localhost:8086.
   */
  public static final String DEFAULT_URI = "http://localhost:8086";

  /**
   * The default InfluxDb database = default.
   */
  public static final String DEFAULT_DATABASE = "default";

  /**
   * The default gzip enabled on InfluxDb = true.
   */
  public static final boolean DEFAULT_COMPRESSION_ENABLED = true;

  /**
   * The default number of threads used = 2.
   */
  public static final int DEFAULT_NUM_THREADS = 2;

  /**
   * The default connection timeout (seconds) = 1.
   */
  public static final int DEFAULT_CONNECT_TIMEOUT = 1;

  /**
   * The default read timeout (seconds) = 10.
   */
  public static final int DEFAULT_READ_TIMEOUT = 10;

  private boolean enabled;
  private String uri;
  private String db;
  private String userName;
  private String password;
  private String retentionPolicy;
  private boolean compressed;
  private int step;
  private int numThreads;
  private int connectTimeout;
  private int readTimeout;
  private int batchSize;

  public VertxInfluxDbOptions() {
    enabled = DEFAULT_ENABLED;
    uri = DEFAULT_URI;
    db = DEFAULT_DATABASE;
    compressed = DEFAULT_COMPRESSION_ENABLED;
    step = DEFAULT_STEP;
    numThreads = DEFAULT_NUM_THREADS;
    connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    readTimeout = DEFAULT_READ_TIMEOUT;
    batchSize = DEFAULT_BATCH_SIZE;
  }

  public VertxInfluxDbOptions(VertxInfluxDbOptions other) {
    enabled = other.enabled;
    uri = other.uri;
    db = other.db;
    userName = other.userName;
    password = other.password;
    retentionPolicy = other.retentionPolicy;
    compressed = other.compressed;
    step = other.step;
    numThreads = other.numThreads;
    connectTimeout = other.connectTimeout;
    readTimeout = other.readTimeout;
    batchSize = other.batchSize;
  }

  public VertxInfluxDbOptions(JsonObject json) {
    this();
    VertxInfluxDbOptionsConverter.fromJson(json, this);
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set true to enable InfluxDB reporting
   */
  public VertxInfluxDbOptions setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public String getUri() {
    return uri;
  }

  /**
   * URI of the InfluxDB server. <i>Example: http://influx:8086</i>.
   */
  public VertxInfluxDbOptions setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getDb() {
    return db;
  }

  /**
   * Database name used to store metrics. Default is "default".
   */
  public VertxInfluxDbOptions setDb(String db) {
    this.db = db;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  /**
   * Username used for authenticated connections
   */
  public VertxInfluxDbOptions setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return password;
  }

  /**
   * Password used for authenticated connections
   */
  public VertxInfluxDbOptions setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getRetentionPolicy() {
    return retentionPolicy;
  }

  /**
   * InfluxDB retention policy
   */
  public VertxInfluxDbOptions setRetentionPolicy(String retentionPolicy) {
    this.retentionPolicy = retentionPolicy;
    return this;
  }

  public boolean isCompressed() {
    return compressed;
  }

  /**
   * Activate or deactivate GZIP compression. It is activated by default.
   */
  public VertxInfluxDbOptions setCompressed(boolean compressed) {
    this.compressed = compressed;
    return this;
  }

  public int getStep() {
    return step;
  }

  /**
   * Push interval steps, in seconds. Default is 10 seconds.
   */
  public VertxInfluxDbOptions setStep(int step) {
    this.step = step;
    return this;
  }

  public int getNumThreads() {
    return numThreads;
  }

  /**
   * Number of threads to use by the push scheduler. Default is 2.
   */
  public VertxInfluxDbOptions setNumThreads(int numThreads) {
    this.numThreads = numThreads;
    return this;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Connection timeout for InfluxDB server connections, in seconds. Default is 1 second.
   */
  public VertxInfluxDbOptions setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  /**
   * Read timeout for InfluxDB server connections, in seconds. Default is 10 seconds.
   */
  public VertxInfluxDbOptions setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Maximum number of measurements sent per request to InfluxDB. When the maximum is reached, several requests are made.
   * Default is 10000.
   */
  public VertxInfluxDbOptions setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  @Override
  public String get(String k) {
    return null;
  }

  @Override
  public String db() {
    return db;
  }

  @Override
  public String userName() {
    return userName;
  }

  @Override
  public String password() {
    return password;
  }

  @Override
  public String retentionPolicy() {
    return retentionPolicy;
  }

  @Override
  public String uri() {
    return uri;
  }

  @Override
  public boolean compressed() {
    return compressed;
  }

  @Override
  public Duration step() {
    return Duration.ofSeconds(step);
  }

  @Override
  public int numThreads() {
    return numThreads;
  }

  @Override
  public Duration connectTimeout() {
    return Duration.ofSeconds(connectTimeout);
  }

  @Override
  public Duration readTimeout() {
    return Duration.ofSeconds(readTimeout);
  }

  @Override
  public int batchSize() {
    return batchSize;
  }
}
