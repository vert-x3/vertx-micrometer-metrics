package io.vertx.kotlin.micrometer

import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.MetricsDomain
import io.vertx.micrometer.backend.VertxInfluxDbOptions
import io.vertx.micrometer.backend.VertxJmxMetricsOptions
import io.vertx.micrometer.backend.VertxPrometheusOptions
import io.vertx.micrometer.match.Match

/**
 * A function providing a DSL for building [io.vertx.micrometer.MicrometerMetricsOptions] objects.
 *
 * Vert.x micrometer configuration.<br/>
 * It is required to set either <code>influxDbOptions</code>, <code>prometheusOptions</code> or {@code jmxMetricsOptions]
 * in order to actually report metrics.
 *
 * @param disabledMetricsCategories  Sets metrics types that are disabled.
 * @param enabled  Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
 * @param influxDbOptions  Set InfluxDB options. Setting a registry backend option is mandatory in order to effectively report metrics.
 * @param jmxMetricsOptions  Set JMX metrics options. Setting a registry backend option is mandatory in order to effectively report metrics.
 * @param labelMatchs  Add a rule for label matching.
 * @param prometheusOptions  Set Prometheus options. Setting a registry backend option is mandatory in order to effectively report metrics.
 * @param registryName  Set a name for the metrics registry, so that a new registry will be created and associated with this name. If <code>registryName</code> is not provided (or null), a default registry will be used. If the same name is given to several Vert.x instances (within the same JVM), they will share the same registry.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.micrometer.MicrometerMetricsOptions original] using Vert.x codegen.
 */
fun MicrometerMetricsOptions(
  disabledMetricsCategories: Iterable<MetricsDomain>? = null,
  enabled: Boolean? = null,
  influxDbOptions: io.vertx.micrometer.backend.VertxInfluxDbOptions? = null,
  jmxMetricsOptions: io.vertx.micrometer.backend.VertxJmxMetricsOptions? = null,
  labelMatchs: Iterable<io.vertx.micrometer.match.Match>? = null,
  prometheusOptions: io.vertx.micrometer.backend.VertxPrometheusOptions? = null,
  registryName: String? = null): MicrometerMetricsOptions = io.vertx.micrometer.MicrometerMetricsOptions().apply {

  if (disabledMetricsCategories != null) {
    this.setDisabledMetricsCategories(disabledMetricsCategories.toSet())
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (influxDbOptions != null) {
    this.setInfluxDbOptions(influxDbOptions)
  }
  if (jmxMetricsOptions != null) {
    this.setJmxMetricsOptions(jmxMetricsOptions)
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

