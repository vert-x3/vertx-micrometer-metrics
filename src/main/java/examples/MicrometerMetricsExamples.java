/*
 * Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package examples;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.docgen.Source;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsService;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxInfluxDbOptions;
import io.vertx.micrometer.VertxJmxMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.ArrayList;

/**
 * @author Joel Takvorian
 */
@Source
public class MicrometerMetricsExamples {
  Vertx vertx;

  public void setupMinimalInfluxDB() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setInfluxDbOptions(new VertxInfluxDbOptions().setEnabled(true))
        .setEnabled(true)));
  }

  public void setupInfluxDBWithUriAndDatabase() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setInfluxDbOptions(new VertxInfluxDbOptions().setEnabled(true)
          .setUri("http://influxdb.example.com:8888")
          .setDb("sales-department"))
        .setEnabled(true)));
  }

  public void setupInfluxDBWithAuthentication() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setInfluxDbOptions(new VertxInfluxDbOptions().setEnabled(true)
          .setUserName("username")
          .setPassword("password"))
        .setEnabled(true)));
  }

  public void setupMinimalPrometheus() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)));
  }

  public void setupPrometheusEmbeddedServer() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(8080))
          .setEmbeddedServerEndpoint("/metrics/vertx"))
        .setEnabled(true)));
  }

  public void setupPrometheusBoundRouter() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)));

    // Later on, creating a router
    Router router = Router.router(vertx);
    router.route("/metrics").handler(routingContext -> {
      PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
      if (prometheusRegistry != null) {
        String response = prometheusRegistry.scrape();
        routingContext.response().end(response);
      } else {
        routingContext.fail(500);
      }
    });
    vertx.createHttpServer().requestHandler(router).listen(8080);
  }

  public void setupMinimalJMX() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setJmxMetricsOptions(new VertxJmxMetricsOptions().setEnabled(true))
        .setEnabled(true)));
  }

  public void setupJMXWithStepAndDomain() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setJmxMetricsOptions(new VertxJmxMetricsOptions().setEnabled(true)
          .setStep(5)
          .setDomain("my.metrics.domain"))
        .setEnabled(true)));
  }

  public void accessDefaultRegistry() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
  }

  public void setupAndAccessCustomRegistry() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setInfluxDbOptions(new VertxInfluxDbOptions().setEnabled(true)) // or VertxPrometheusOptions
        .setRegistryName("my registry")
        .setEnabled(true)));

    // Later on:
    MeterRegistry registry = BackendRegistries.getNow("my registry");
  }

  public void customTimerExample() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    Timer timer = Timer
      .builder("my.timer")
      .description("a description of what this timer does")
      .register(registry);

    vertx.setPeriodic(1000, l -> {
      timer.record(() -> {
        // Running here some operation to monitor
      });
    });
  }

  public void instrumentJVM() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();

    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
  }

  public void setupWithMatcherForFiltering() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .addLabelMatch(new Match()
          // Restrict HTTP server metrics to those with label "local=localhost:8080" only
          .setDomain(MetricsDomain.HTTP_SERVER)
          .setLabel("local")
          .setValue("localhost:8080"))
        .setEnabled(true)));
  }

  public void setupWithMatcherReset() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setLabelMatchs(new ArrayList<>())
        .setEnabled(true)));
  }

  public void setupWithMatcherForIgnoring() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .addLabelMatch(new Match()
          // Set all values for "remote" label to "_", for all domains. In other words, it's like disabling the "remote" label.
          .setLabel("remote")
          .setType(MatchType.REGEX)
          .setValue(".*")
          .setAlias("_"))
        .setEnabled(true)));
  }

  public void useMicrometerFilters() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();

    registry.config().meterFilter(MeterFilter.ignoreTags("address", "remote"))
      .meterFilter(MeterFilter.renameTag("vertx.verticle", "deployed", "instances"));
  }

  public void createFullSnapshot() {
    MetricsService metricsService = MetricsService.create(vertx);
    JsonObject metrics = metricsService.getMetricsSnapshot();
    System.out.println(metrics);
  }

  public void createPartialSnapshot() {
    HttpServer server = vertx.createHttpServer();
    MetricsService metricsService = MetricsService.create(server);
    JsonObject metrics = metricsService.getMetricsSnapshot();
    System.out.println(metrics);
  }

  public void createSnapshotFromPrefix() {
    MetricsService metricsService = MetricsService.create(vertx);
    // Client + server
    JsonObject metrics = metricsService.getMetricsSnapshot("vertx.http");
    System.out.println(metrics);
  }

  public void setupWithCompositeRegistry() {
    CompositeMeterRegistry myRegistry = new CompositeMeterRegistry();
    myRegistry.add(new JmxMeterRegistry(s -> null, Clock.SYSTEM));
    myRegistry.add(new GraphiteMeterRegistry(s -> null, Clock.SYSTEM));

    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setMicrometerRegistry(myRegistry)
        .setEnabled(true)));
  }
}
