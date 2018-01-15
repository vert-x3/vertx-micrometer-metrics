package io.vertx.kotlin.ext.monitoring.collector

import io.vertx.ext.monitoring.collector.BatchingReporterOptions
import io.vertx.ext.monitoring.common.MetricsCategory

/**
 * A function providing a DSL for building [io.vertx.ext.monitoring.collector.BatchingReporterOptions] objects.
 *
 * Common options for reporters sending metrics in batches.
 *
 * @param batchDelay  Set the maximum delay between two consecutive batches (in seconds). To reduce the number of HTTP exchanges, metric data is sent by the reporter in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>1</code> second.
 * @param batchSize  Set the maximum number of metrics in a batch. To reduce the number of HTTP exchanges, metric data is sent by the reporter in batches. A batch is sent as soon as the number of metrics collected reaches the configured <code>batchSize</code>, or after the <code>batchDelay</code> expires. Defaults to <code>50</code>.
 * @param disabledMetricsCategories 
 * @param enabled 
 * @param metricsBridgeAddress 
 * @param metricsBridgeEnabled 
 * @param prefix  Set the metric name prefix. Metric names are not prefixed by default. Prefixing metric names is required to distinguish data sent by different Vert.x instances.
 * @param schedule  Set the metric collection interval (in seconds). Defaults to <code>1</code>.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.ext.monitoring.collector.BatchingReporterOptions original] using Vert.x codegen.
 */
fun BatchingReporterOptions(
  batchDelay: Int? = null,
  batchSize: Int? = null,
  disabledMetricsCategories: Iterable<MetricsCategory>? = null,
  enabled: Boolean? = null,
  metricsBridgeAddress: String? = null,
  metricsBridgeEnabled: Boolean? = null,
  prefix: String? = null,
  schedule: Int? = null): BatchingReporterOptions = io.vertx.ext.monitoring.collector.BatchingReporterOptions().apply {

  if (batchDelay != null) {
    this.setBatchDelay(batchDelay)
  }
  if (batchSize != null) {
    this.setBatchSize(batchSize)
  }
  if (disabledMetricsCategories != null) {
    this.setDisabledMetricsCategories(disabledMetricsCategories.toSet())
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (metricsBridgeAddress != null) {
    this.setMetricsBridgeAddress(metricsBridgeAddress)
  }
  if (metricsBridgeEnabled != null) {
    this.setMetricsBridgeEnabled(metricsBridgeEnabled)
  }
  if (prefix != null) {
    this.setPrefix(prefix)
  }
  if (schedule != null) {
    this.setSchedule(schedule)
  }
}

