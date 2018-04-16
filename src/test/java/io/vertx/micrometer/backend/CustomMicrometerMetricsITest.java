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
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class CustomMicrometerMetricsITest {

  private static final String REGITRY_NAME = "CustomMicrometerMetricsITest";
  private Vertx vertx;

  @After
  public void tearDown() {
    BackendRegistries.stop(REGITRY_NAME);
  }

  @Test
  public void shouldReportWithCompositeRegistry(TestContext context) throws Exception {
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
    }, Clock.SYSTEM));

    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setMicrometerRegistry(myRegistry)
        .setRegistryName(REGITRY_NAME)
        .addDisabledMetricsCategory(MetricsDomain.HTTP_SERVER)
        .addDisabledMetricsCategory(MetricsDomain.NAMED_POOLS)
        .setEnabled(true)));

    // Mock an influxdb server
    Async asyncInflux = context.async();
    simulateInfluxServer(context, body -> {
      try {
        context.verify(w -> assertThat(body)
          .contains("vertx_eventbus_handlers,address=test-eb,metric_type=gauge value=1"));
      } finally {
        asyncInflux.complete();
      }
    });

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Read MBean
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    assertThat(mbs.getDomains()).contains("metrics");
    Number result = (Number) mbs.getAttribute(new ObjectName("metrics", "name", "vertxEventbusHandlers.address.test-eb"), "Value");
    assertThat(result).isEqualTo(1d);

    // Await influx
    asyncInflux.awaitSuccess();
  }

  private void simulateInfluxServer(TestContext context, Consumer<String> onRequest) {
    vertx.runOnContext(v -> {
      vertx.createHttpServer(new HttpServerOptions()
        .setCompressionSupported(true)
        .setDecompressionSupported(true)
        .setLogActivity(true)
        .setHost("localhost")
        .setPort(8087))
        .requestHandler(req -> {
          req.exceptionHandler(context.exceptionHandler());
          Buffer fullRequestBody = Buffer.buffer();
          req.handler(fullRequestBody::appendBuffer);
          req.endHandler(h -> {
            String str = fullRequestBody.toString();
            if (str.isEmpty()) {
              req.response().setStatusCode(200).end();
              return;
            }
            try {
              onRequest.accept(str);
            } finally {
              req.response().setStatusCode(200).end();
            }
          });
        }).listen(8087, "localhost", context.asyncAssertSuccess());
    });
  }
}
