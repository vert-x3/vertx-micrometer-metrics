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

package io.vertx.ext.monitoring.collector;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.monitoring.common.MetricsOptionsBase;

/**
 * Common options for reporters sending metrics in batches.
 *
 * @author Thomas Segismont
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class BatchingReporterOptions extends MetricsOptionsBase {

  /**
   * The default metric name prefix (empty).
   */
  private static final String DEFAULT_PREFIX = "";

  /**
   * Default value for metric collection interval (in seconds) = 1.
   */
  private static final int DEFAULT_SCHEDULE = 1;

  /**
   * Default value for the maximum number of metrics in a batch = 50.
   */
  private static final int DEFAULT_BATCH_SIZE = 50;

  /**
   * Default value for the maximum delay between two consecutive batches (in seconds) = 1.
   */
  private static final int DEFAULT_BATCH_DELAY = 1;

  private String prefix;
  private int schedule;
  private int batchSize;
  private int batchDelay;

  public BatchingReporterOptions() {
    prefix = DEFAULT_PREFIX;
    schedule = DEFAULT_SCHEDULE;
    batchSize = DEFAULT_BATCH_SIZE;
    batchDelay = DEFAULT_BATCH_DELAY;
  }

  public BatchingReporterOptions(BatchingReporterOptions other) {
    super(other);
    prefix = other.prefix;
    schedule = other.schedule;
    batchSize = other.batchSize;
    batchDelay = other.batchDelay;
  }

  public BatchingReporterOptions(JsonObject json) {
    this();
    BatchingReporterOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the metric name prefix
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * Set the metric name prefix. Metric names are not prefixed by default. Prefixing metric names is required to
   * distinguish data sent by different Vert.x instances.
   */
  public BatchingReporterOptions setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * @return the metric collection interval (in seconds)
   */
  public int getSchedule() {
    return schedule;
  }

  /**
   * Set the metric collection interval (in seconds). Defaults to {@code 1}.
   */
  public BatchingReporterOptions setSchedule(int schedule) {
    this.schedule = schedule;
    return this;
  }

  /**
   * @return the maximum number of metrics in a batch
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent by the
   * reporter in batches. A batch is sent as soon as the number of metrics collected reaches the configured
   * {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 50}.
   */
  public BatchingReporterOptions setBatchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  /**
   * @return the maximum delay between two consecutive batches
   */
  public int getBatchDelay() {
    return batchDelay;
  }

  /**
   * Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric
   * data is sent by the reporter in batches. A batch is sent as soon as the number of metrics collected reaches
   * the configured {@code batchSize}, or after the {@code batchDelay} expires. Defaults to {@code 1} second.
   */
  public BatchingReporterOptions setBatchDelay(int batchDelay) {
    this.batchDelay = batchDelay;
    return this;
  }
}
