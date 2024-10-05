module io.vertx.metrics.micrometer {

  requires static io.vertx.docgen;
  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;

  requires io.prometheus.metrics.model;
  requires io.vertx.core;
  requires io.vertx.core.logging;
  requires io.vertx.web;
  requires micrometer.core;
  requires micrometer.registry.graphite;
  requires micrometer.registry.influx;
  requires micrometer.registry.jmx;
  requires micrometer.registry.prometheus;

  exports io.vertx.micrometer;
  exports io.vertx.micrometer.backends;

  provides io.vertx.core.spi.VertxServiceProvider with io.vertx.micrometer.MicrometerMetricsFactory;

}
