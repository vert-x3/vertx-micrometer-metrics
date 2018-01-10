package io.vertx.kotlin.monitoring.backend

import io.vertx.monitoring.backend.VertxPrometheusOptions

/**
 * A function providing a DSL for building [io.vertx.monitoring.backend.VertxPrometheusOptions] objects.
 *
 * Options for Prometheus metrics backend.
 *
 * @param embeddedServerEndpoint  Set metrics endpoint. Use conjointly with the embedded server options.
 * @param embeddedServerOptions  An embedded server will start to expose metrics with Prometheus format
 * @param enabled  Set whether backend will be enabled on the Vert.x instance.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.monitoring.backend.VertxPrometheusOptions original] using Vert.x codegen.
 */
fun PrometheusOptions(
  embeddedServerEndpoint: String? = null,
  embeddedServerOptions: io.vertx.core.http.HttpServerOptions? = null,
  enabled: Boolean? = null): VertxPrometheusOptions = VertxPrometheusOptions().apply {

  if (embeddedServerEndpoint != null) {
    this.setEmbeddedServerEndpoint(embeddedServerEndpoint)
  }
  if (embeddedServerOptions != null) {
    this.setEmbeddedServerOptions(embeddedServerOptions)
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
}

