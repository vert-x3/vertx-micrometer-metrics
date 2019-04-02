/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.micrometer.backend;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MatchType;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class PrometheusMetricsITest {

  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldStartEmbeddedServer(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addLabels(Label.LOCAL, Label.HTTP_PATH, Label.REMOTE)
        .setEnabled(true)));

    Async async = context.async();
    // First "blank" connection to trigger some metrics
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", r1 -> {
      // Delay to make "sure" metrics are populated
      vertx.setTimer(500, l ->
        // Second connection, this time actually reading the metrics content
        PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", body -> {
            context.verify(v2 -> assertThat(body.toString())
              .contains("vertx_http_client_requests{local=\"?\",method=\"GET\",path=\"/metrics\",remote=\"localhost:9090\"")
              .doesNotContain("vertx_http_client_responseTime_seconds_bucket"));
            async.complete();
        }));
    });
    async.awaitSuccess(10000);
  }

  @Test
  public void shouldBindExistingServer(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)));

    Router router = Router.router(vertx);
    router.route("/custom").handler(routingContext -> {
      PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
      String response = prometheusRegistry.scrape();
      routingContext.response().end(response);
    });
    vertx.createHttpServer().requestHandler(router).exceptionHandler(context.exceptionHandler()).listen(8081);

    Async async = context.async();
    PrometheusTestHelper.tryConnect(vertx, context, 8081, "localhost", "/custom", body -> {
      context.verify(v -> assertThat(body.toString())
            .contains("vertx_http_"));
      async.complete();
    });
    async.awaitSuccess(10000);
  }

  @Test
  public void shouldExcludeCategory(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addDisabledMetricsCategory(MetricsDomain.HTTP_SERVER)
        .addLabels(Label.LOCAL, Label.REMOTE)
        .setEnabled(true)));

    Async async = context.async();
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", body -> {
      context.verify(v -> assertThat(body.toString())
        .contains("vertx_http_client_connections{local=\"?\",remote=\"localhost:9090\",} 1.0")
        .doesNotContain("vertx_http_server_connections{local=\"0.0.0.0:9090\",remote=\"_\",} 1.0"));
      async.complete();
    });
    async.awaitSuccess(10000);
  }

  @Test
  public void shouldExposeEventBusMetrics(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addLabels(Label.EB_ADDRESS)
        .setEnabled(true)));

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.awaitSuccess(15000);

    // Read metrics on HTTP endpoint for eventbus metrics
    Async async = context.async();
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", body -> {
      context.verify(v -> assertThat(body.toString())
        .contains("vertx_eventbus_published_total{address=\"test-eb\",side=\"local\",} 1.0",
          "vertx_eventbus_received_total{address=\"test-eb\",side=\"local\",} 1.0",
          "vertx_eventbus_handlers{address=\"test-eb\",} 1.0",
          "vertx_eventbus_delivered_total{address=\"test-eb\",side=\"local\",} 1.0",
          "vertx_eventbus_processingTime_seconds_count{address=\"test-eb\",} 1.0"));
      async.complete();
    });
    async.awaitSuccess(15000);
  }

  @Test
  public void shouldPublishPercentileStats(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setPublishQuantiles(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addLabels(Label.LOCAL, Label.HTTP_PATH, Label.REMOTE, Label.HTTP_CODE)
        .setEnabled(true)));

    Async async = context.async();
    // First "blank" connection to trigger some metrics
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", r1 -> {
      // Delay to make "sure" metrics are populated
      vertx.setTimer(500, l ->
        // Second connection, this time actually reading the metrics content
        PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", body -> {
          context.verify(v2 -> assertThat(body.toString())
            .contains("vertx_http_client_responseTime_seconds_bucket{code=\"200\""));
          async.complete();
        }));
    });
    async.awaitSuccess(10000);
  }

  @Test
  public void canMatchLabels(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setStartEmbeddedServer(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addLabels(Label.HTTP_PATH)
        .addLabelMatch(new Match()
          .setDomain(MetricsDomain.HTTP_CLIENT)
          .setValue(".*")
          .setLabel(Label.HTTP_PATH.toString())
          .setType(MatchType.REGEX))
        .setEnabled(true)));

    Async async = context.async();
    // First "blank" connection to trigger some metrics
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", r1 -> {
      // Delay to make "sure" metrics are populated
      vertx.setTimer(500, l ->
        // Second connection, this time actually reading the metrics content
        PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", body -> {
          context.verify(v2 -> assertThat(body.toString())
            .contains("vertx_http_client_requests{method=\"GET\",path=\"/metrics\",}")
            .doesNotContain("vertx_http_client_responseTime_seconds_bucket"));
          async.complete();
        }));
    });
    async.awaitSuccess(10000);
  }
}
