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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class CustomMicrometerMetricsITest extends MicrometerMetricsTestBase {

  private Vertx vertxForSimulatedServer = Vertx.vertx();

  @Override
  protected void tearDown(TestContext context) {
    super.tearDown(context);
    vertxForSimulatedServer.close().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportWithCompositeRegistry(TestContext context) throws Exception {
    // Mock an influxdb server
    Async asyncInflux = context.async();
    InfluxDbTestHelper.simulateInfluxServer(vertxForSimulatedServer, context, 8087, body -> {
      try {
        context.verify(w -> assertThat(body)
          .contains("vertx_eventbus_handlers,address=test-eb,metric_type=gauge value=1"));
      } finally {
        asyncInflux.complete();
      }
    });

    CompositeMeterRegistry myRegistry = new CompositeMeterRegistry();
    myRegistry.add(new JmxMeterRegistry(s -> null, Clock.SYSTEM));
    myRegistry.add(new InfluxMeterRegistry(new InfluxConfig() {
      @Override
      public String get(String s) {
        return null;
      }
      @Override
      public Duration step() {
        return Duration.ofSeconds(1);
      }

      @Override
      public String uri() {
        return "http://localhost:8087";
      }

      @Override
      public boolean autoCreateDb() {
        return false;
      }
    }, Clock.SYSTEM));

    meterRegistry = myRegistry;

    metricsOptions = new MicrometerMetricsOptions()
      .setRegistryName(registryName)
      .addDisabledMetricsCategory(MetricsDomain.HTTP_SERVER)
      .addDisabledMetricsCategory(MetricsDomain.NAMED_POOLS)
      .addLabels(Label.EB_ADDRESS)
      .setEnabled(true);

    vertx = vertx(context);

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Read MBean
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    assertThat(mbs.getDomains()).contains("metrics");
    Hashtable<String, String> table = new Hashtable<>();
    table.put("type", "gauges");
    table.put("name", "vertxEventbusHandlers.address.test-eb");
    Number result = (Number) mbs.getAttribute(new ObjectName("metrics", table), "Value");
    assertThat(result).isEqualTo(1d);

    // Await influx
    asyncInflux.awaitSuccess();
  }

  @Test
  public void shouldPublishQuantilesWithProvidedRegistry(TestContext context) throws Exception {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    meterRegistry = registry;
    metricsOptions = new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)
        .setPublishQuantiles(true)
        .setStartEmbeddedServer(true)
        .setEmbeddedServerOptions(new HttpServerOptions().setPort(9090)))
      .setEnabled(true);

    vertx = vertx(context);

    Async async = context.async();
    // Dummy connection to trigger some metrics
    PrometheusTestHelper.tryConnect(vertx, context, 9090, "localhost", "/metrics", r1 -> {
      // Delay to make "sure" metrics are populated
      vertx.setTimer(500, l -> {
        assertThat(registry.scrape()).contains("vertx_http_client_response_time_seconds_bucket{code=\"200\"");
        async.complete();
      });
    });
    async.awaitSuccess(10000);
  }
}
