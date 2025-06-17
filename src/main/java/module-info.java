module io.vertx.metrics.micrometer {

  requires static io.vertx.docgen;
  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;

  requires io.netty.buffer;
  requires io.vertx.core;
  requires io.vertx.core.logging;
  requires io.vertx.web;
  requires micrometer.core;

  // Required only at compilation (users can pick the backends they want)
  requires static micrometer.registry.graphite;
  requires static micrometer.registry.influx;
  requires static micrometer.registry.jmx;
  requires static micrometer.registry.prometheus;
  requires static io.prometheus.metrics.model;

  exports io.vertx.micrometer;
  exports io.vertx.micrometer.backends;

  provides io.vertx.core.spi.VertxServiceProvider with io.vertx.micrometer.MicrometerMetricsFactory;

}
