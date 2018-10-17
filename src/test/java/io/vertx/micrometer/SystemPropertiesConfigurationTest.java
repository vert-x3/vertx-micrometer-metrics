/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.micrometer;

import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static io.vertx.core.impl.launcher.commands.BareCommand.METRICS_OPTIONS_PROP_PREFIX;
import static io.vertx.micrometer.impl.VertxMetricsFactoryImpl.*;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @author Thomas Segismont
 */
@RunWith(VertxUnitRunner.class)
public class SystemPropertiesConfigurationTest {

  private volatile Vertx vertx;
  private volatile VertxOptions vertxOptions;

  @After
  public void tearDown(TestContext context) {
    System.getProperties().stringPropertyNames().forEach(name -> {
      if (name.startsWith(METRICS_OPTIONS_PROP_PREFIX)) {
        System.clearProperty(name);
      }
    });
    if (vertx != null) {
      vertx.close(context.asyncAssertSuccess());
    }
  }

  @Test
  public void testPrometheusDefaults(TestContext context) {
    System.setProperty(METRICS_OPTIONS_PROP_PREFIX + "enabled", String.valueOf(true));
    System.setProperty(PROMETHEUS_ENABLED, String.valueOf(true));
    testPrometheus(context, PROMETHEUS_DEFAULT_PORT);
  }

  @Test
  public void testPrometheusPort(TestContext context) {
    int port = 8888;
    System.setProperty(METRICS_OPTIONS_PROP_PREFIX + "enabled", String.valueOf(true));
    System.setProperty(PROMETHEUS_ENABLED, String.valueOf(true));
    System.setProperty(PROMETHEUS_PORT, String.valueOf(port));
    testPrometheus(context, port);
  }

  private void testPrometheus(TestContext context, int expectedPort) {
    startVertx(context);

    VertxPrometheusOptions prometheusOptions = getVertxPrometheusOptions();

    HttpServerOptions embeddedServerOptions = prometheusOptions.getEmbeddedServerOptions();
    assertNotNull(embeddedServerOptions);
    assertEquals(expectedPort, embeddedServerOptions.getPort());
  }

  @Test
  public void testJvmMetricsEnabled(TestContext context) {
    System.setProperty(METRICS_OPTIONS_PROP_PREFIX + "enabled", String.valueOf(true));
    System.setProperty(PROMETHEUS_ENABLED, String.valueOf(true));
    System.setProperty(JVM_METRICS_ENABLED, String.valueOf(true));

    startVertx(context);

    VertxPrometheusOptions prometheusOptions = getVertxPrometheusOptions();

    HttpClientOptions httpClientOptions = new HttpClientOptions()
      .setDefaultPort(prometheusOptions.getEmbeddedServerOptions().getPort());

    Async async = context.async();
    Set<String> metrics = Collections.synchronizedSet(new HashSet<>());
    HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
    httpClient.get(prometheusOptions.getEmbeddedServerEndpoint()).handler(resp -> {
      RecordParser parser = RecordParser.newDelimited("\n", resp);
      parser.exceptionHandler(context::fail).endHandler(v -> {
        async.countDown();
      }).handler(buffer -> {
        String line = buffer.toString();
        if (line.startsWith("# TYPE")) {
          metrics.add(line.split(" ")[2]);
        }
      });
    }).exceptionHandler(context::fail).end();

    async.await(3000);

    Stream<String> expectedMetrics = Stream.<String>builder()
      .add("jvm_classes_loaded") // from classloader metrics
      .add("jvm_buffer_count") // from JVM memory metrics
      .add("system_cpu_count") // from processor metrics
      .add("jvm_threads_live") // from JVM thread metrics
      .build();
    assertTrue(metrics.toString(), metrics.containsAll(expectedMetrics.collect(toList())));
  }

  private void startVertx(TestContext context) {
    MyLauncher myLauncher = new MyLauncher(context);
    myLauncher.dispatch(new String[]{"run", "java:" + MyVerticle.class.getName()});
    myLauncher.await();
  }

  private VertxPrometheusOptions getVertxPrometheusOptions() {
    MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
    assertThat(metricsOptions, instanceOf(MicrometerMetricsOptions.class));

    MicrometerMetricsOptions options = (MicrometerMetricsOptions) metricsOptions;
    VertxPrometheusOptions prometheusOptions = options.getPrometheusOptions();
    assertNotNull(prometheusOptions);
    assertTrue(prometheusOptions.isEnabled());
    assertTrue(prometheusOptions.isStartEmbeddedServer());

    return prometheusOptions;
  }

  private class MyLauncher extends Launcher {

    Async startAsync;

    MyLauncher(TestContext context) {
      startAsync = context.async(2);
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
      SystemPropertiesConfigurationTest.this.vertx = vertx;
      startAsync.countDown();
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
      SystemPropertiesConfigurationTest.this.vertxOptions = options;
      startAsync.countDown();
    }

    void await() {
      startAsync.await(3000);
    }
  }
}
