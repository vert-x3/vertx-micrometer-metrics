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

package io.vertx.monitoring.backend;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.monitoring.MetricsCategory;
import io.vertx.monitoring.VertxMonitoringOptions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class PrometheusMetricsITest {

  private Vertx vertx;

  @After
  public void tearDown() {
    BackendRegistries.stop(VertxMonitoringOptions.DEFAULT_REGISTRY_NAME);
  }

  @Test
  public void shouldStartEmbeddedServer(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .setEnabled(true)));

    Async async = context.async();
    HttpClientRequest req = vertx.createHttpClient()
      .get(9090, "localhost", "/metrics")
      .handler(res -> {
        context.assertEquals(200, res.statusCode());
        res.bodyHandler(body -> {
          context.verify(v -> assertThat(body.toString())
            .contains("vertx_http_server_connections{local=\"0.0.0.0:9090\",remote=\"_\",} 1.0"));
          async.complete();
        });
      });
    req.end();
  }

  @Test
  public void shouldBindExistingServer(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)));

    Router router = Router.router(vertx);
    router.route("/custom").handler(routingContext -> {
      PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
      String response = prometheusRegistry.scrape();
      routingContext.response().end(response);
    });
    vertx.createHttpServer().requestHandler(router::accept).listen(8081);

    Async async = context.async();
    HttpClientRequest req = vertx.createHttpClient()
      .get(8081, "localhost", "/custom")
      .handler(res -> {
        context.assertEquals(200, res.statusCode());
        res.bodyHandler(body -> {
          context.verify(v -> assertThat(body.toString())
            .contains("vertx_http_server_connections{local=\"0.0.0.0:8081\",remote=\"_\",} 1.0"));
          async.complete();
        });
      });
    req.end();
  }

  @Test
  public void shouldExcludeCategory(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .addDisabledMetricsCategory(MetricsCategory.HTTP_SERVER)
        .setEnabled(true)));

    Async async = context.async();
    HttpClientRequest req = vertx.createHttpClient()
      .get(9090, "localhost", "/metrics")
      .handler(res -> {
        context.assertEquals(200, res.statusCode());
        res.bodyHandler(body -> {
          context.verify(v -> assertThat(body.toString())
            .contains("vertx_http_client_connections{local=\"?\",remote=\"localhost:9090\",} 1.0")
            .doesNotContain("vertx_http_server_connections{local=\"0.0.0.0:9090\",remote=\"_\",} 1.0"));
          async.complete();
        });
      });
    req.end();
  }

  @Test
  public void shouldExposeEventBusMetrics(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
          .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
        .setEnabled(true)));

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Read metrics on HTTP endpoint for eventbus metrics
    Async async = context.async();
    HttpClientRequest req = vertx.createHttpClient()
      .get(9090, "localhost", "/metrics")
      .handler(res -> {
        context.assertEquals(200, res.statusCode());
        res.bodyHandler(body -> {
          String str = body.toString();
          context.verify(v -> assertThat(str)
            .contains("vertx_eventbus_published_total{address=\"test-eb\",side=\"local\",} 1.0",
              "vertx_eventbus_received_total{address=\"test-eb\",side=\"local\",} 1.0",
              "vertx_eventbus_handlers{address=\"test-eb\",} 1.0",
              "vertx_eventbus_delivered_total{address=\"test-eb\",side=\"local\",} 1.0",
              "vertx_eventbus_processingTime_duration_seconds_count{address=\"test-eb\",} 1.0"));
          async.complete();
        });
      });
    req.end();
  }
}
