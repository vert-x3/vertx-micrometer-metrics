package io.vertx.micrometer;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;

/**
 * A request handler for serving Prometheus metrics.
 *
 * @author Swamy Mavuri
 */
public class PrometheusRequestHandler implements Handler<HttpServerRequest> {

  private final PrometheusMeterRegistry registry;
  private final VertxPrometheusOptions options;

  /**
   * The handler requires a {@link io.micrometer.prometheus.PrometheusMeterRegistry} and a
   * {@link VertxPrometheusOptions} instance to be constructed.
   * Registry is used to scrape the metrics.
   * Options provide the configuration for the metrics endpoint.
   */
  public PrometheusRequestHandler(PrometheusMeterRegistry registry, VertxPrometheusOptions options) {
    this.registry = registry;
    this.options = options;
  }


  @Override
  public void handle(HttpServerRequest request) {
    if (options.getEmbeddedServerEndpoint().equals(request.path())) {
      request.response().putHeader(HttpHeaders.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004).end(registry.scrape());
    } else {
      request.response().setStatusCode(404).end();
    }
  }
}

