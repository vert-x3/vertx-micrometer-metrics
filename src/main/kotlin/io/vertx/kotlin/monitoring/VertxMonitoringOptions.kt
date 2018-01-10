package io.vertx.kotlin.monitoring

import io.vertx.monitoring.VertxMonitoringOptions
import io.vertx.monitoring.MetricsCategory
import io.vertx.monitoring.backend.VertxInfluxDbOptions
import io.vertx.monitoring.backend.VertxPrometheusOptions
import io.vertx.monitoring.match.Match

/**
 * A function providing a DSL for building [io.vertx.monitoring.VertxMonitoringOptions] objects.
 *
 * Vert.x monitoring configuration.<br/>
 * It is required to set either <code>influxDbOptions</code> or <code>prometheusOptions</code>, but not both,
 * in order to actually report metrics.
 *
 * @param disabledMetricsCategories  Sets metrics types that are disabled.
 * @param enabled  Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
 * @param influxDbOptions  Set InfluxDB options. Setting either InfluxDB or Prometheus options is mandatory in order to effectively report metrics.
 * @param labelMatchs  Add a rule for label matching.
 * @param prometheusOptions  Set Prometheus options. Setting either InfluxDB or Prometheus options is mandatory in order to effectively report metrics.
 * @param registryName  Set a name for the metrics registry, so that a new registry will be created and associated with this name. If <code>registryName</code> is not provided (or null), a default registry will be used. If the same name is given to several Vert.x instances (within the same JVM), they will share the same registry.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.monitoring.VertxMonitoringOptions original] using Vert.x codegen.
 */
fun VertxMonitoringOptions(
  disabledMetricsCategories: Iterable<MetricsCategory>? = null,
  enabled: Boolean? = null,
  influxDbOptions: io.vertx.monitoring.backend.VertxInfluxDbOptions? = null,
  labelMatchs: Iterable<io.vertx.monitoring.match.Match>? = null,
  prometheusOptions: io.vertx.monitoring.backend.VertxPrometheusOptions? = null,
  registryName: String? = null): VertxMonitoringOptions = io.vertx.monitoring.VertxMonitoringOptions().apply {

  if (disabledMetricsCategories != null) {
    this.setDisabledMetricsCategories(disabledMetricsCategories.toSet())
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (influxDbOptions != null) {
    this.setInfluxDbOptions(influxDbOptions)
  }
  if (labelMatchs != null) {
    for (item in labelMatchs) {
      this.addLabelMatch(item)
    }
  }
  if (prometheusOptions != null) {
    this.setPrometheusOptions(prometheusOptions)
  }
  if (registryName != null) {
    this.setRegistryName(registryName)
  }
}

