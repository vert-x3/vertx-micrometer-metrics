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
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.docgen.Source;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.*;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    router.route("/metrics").handler(PrometheusScrapingHandler.create());
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

  public void setupWithLabelsEnabled() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setLabels(EnumSet.of(Label.REMOTE, Label.LOCAL, Label.HTTP_CODE, Label.HTTP_PATH))
        .setEnabled(true)));
  }

  public void useMicrometerFilters() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    Pattern pattern = Pattern.compile("/foo/bar/.*");

    registry.config().meterFilter(
      MeterFilter.replaceTagValues(Label.HTTP_PATH.toString(), actualPath -> {
        Matcher m = pattern.matcher(actualPath);
        if (m.matches()) {
          return "/foo/bar/:id";
        }
        return actualPath;
      }, ""));
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

  public void enableQuantiles() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setPublishQuantiles(true))
        .setEnabled(true)));
  }

  public void enableLimitedQuantiles() {
    PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    registry.config().meterFilter(
        new MeterFilter() {
          @Override
          public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
            return DistributionStatisticConfig.builder()
                .percentiles(0.95, 0.99)
                .build()
                .merge(config);
          }
        });
  }

  public void useExistingRegistry() {
    // This registry might be used to collect metrics other than Vert.x ones
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // You could also reuse an existing registry from the Prometheus Java client:
    CollectorRegistry prometheusClientRegistry = new CollectorRegistry();
    registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusClientRegistry, Clock.SYSTEM);

    // It's reused in MicrometerMetricsOptions.
    // Prometheus options configured here, such as "setPublishQuantiles(true)", will affect the whole registry.
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setPublishQuantiles(true))
        .setMicrometerRegistry(registry)
        .setEnabled(true)));
  }

  public void useV3CompatNames() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setMetricsNaming(MetricsNaming.v3Names())
        .setEnabled(true)));
  }

  public void useCustomTagsProvider() {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setRequestsTagsProvider(req -> {
          String user = req.headers().get("x-user");
          return Collections.singletonList(Tag.of("user", user));
        })
        .setEnabled(true)));
  }
}
