package io.vertx.kotlin.monitoring.backend

import io.vertx.monitoring.backend.VertxPrometheusOptions
import io.vertx.core.http.HttpServerOptions

/**
 * A function providing a DSL for building [io.vertx.monitoring.backend.VertxPrometheusOptions] objects.
 *
 * Options for Prometheus metrics backend.
 *
 * @param embeddedServerEndpoint  Set metrics endpoint. Use conjointly with the embedded server options. Defaults to <i>/metrics</i>.
 * @param embeddedServerOptions  HTTP server options for the embedded server
 * @param enabled  Set true to enable Prometheus reporting
 * @param startEmbeddedServer  When true, an embedded server will start to expose metrics with Prometheus format.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [io.vertx.monitoring.backend.VertxPrometheusOptions original] using Vert.x codegen.
 */
fun VertxPrometheusOptions(
  embeddedServerEndpoint: String? = null,
  embeddedServerOptions: io.vertx.core.http.HttpServerOptions? = null,
  enabled: Boolean? = null,
  startEmbeddedServer: Boolean? = null): VertxPrometheusOptions = io.vertx.monitoring.backend.VertxPrometheusOptions().apply {

  if (embeddedServerEndpoint != null) {
    this.setEmbeddedServerEndpoint(embeddedServerEndpoint)
  }
  if (embeddedServerOptions != null) {
    this.setEmbeddedServerOptions(embeddedServerOptions)
  }
  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (startEmbeddedServer != null) {
    this.setStartEmbeddedServer(startEmbeddedServer)
  }
}

