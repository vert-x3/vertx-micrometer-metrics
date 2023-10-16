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
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.backend.PrometheusTestHelper;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Segismont
 */
@RunWith(VertxUnitRunner.class)
public class ExternalConfigurationTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private volatile Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    if (vertx != null) {
      vertx.close().onComplete(context.asyncAssertSuccess());
    }
  }

  @Test
  public void testPrometheusDefaults(TestContext context) throws Exception {
    VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
      .setEnabled(true)
      .setStartEmbeddedServer(true)
      .setEmbeddedServerOptions(new HttpServerOptions().setPort(9999));
    MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
      .setEnabled(true)
      .setPrometheusOptions(prometheusOptions);

    startVertx(context, metricsOptions);

    Set<String> metrics = PrometheusTestHelper.getMetricNames(vertx, context, 9999, "localhost", "/metrics", 3000);
    assertThat(metrics).contains("vertx_http_client_active_connections")
      .doesNotContain("jvm_classes_loaded");
  }

  @Test
  public void testJvmMetricsEnabled(TestContext context) throws Exception {
    VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
      .setEnabled(true)
      .setStartEmbeddedServer(true)
      .setEmbeddedServerOptions(new HttpServerOptions().setPort(9999));
    MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
      .setEnabled(true)
      .setJvmMetricsEnabled(true)
      .setPrometheusOptions(prometheusOptions);

    startVertx(context, metricsOptions);

    Set<String> metrics = PrometheusTestHelper.getMetricNames(vertx, context, 9999, "localhost", "/metrics", 3000);
    assertThat(metrics).contains(
      "jvm_classes_loaded_classes", // from ClassLoaderMetrics
      "jvm_compilation_time_ms_total", // from JvmCompilationMetrics
      "jvm_gc_memory_promoted_bytes_total", // from JvmGcMetrics
      "jvm_gc_overhead_percent", // from JvmHeapPressureMetrics
      "jvm_info", // from JvmInfoMetrics
      "jvm_buffer_count_buffers", // from JvmMemoryMetrics
      "jvm_threads_live_threads", // from JvmThreadMetrics
      "system_cpu_count", // from ProcessorMetrics
      "process_uptime_seconds" // from UptimeMetrics
    );
  }

  private void startVertx(TestContext context, MicrometerMetricsOptions metricsOptions) throws Exception {
    JsonObject json = new JsonObject()
      .put("metricsOptions", metricsOptions.toJson());

    File optionsFile = temporaryFolder.newFile();
    Files.write(optionsFile.toPath(), json.toBuffer().getBytes());

    MyLauncher myLauncher = new MyLauncher(context);
    myLauncher.dispatch(new String[]{"run", "java:" + MyVerticle.class.getName(), "-options", optionsFile.getPath()});
    myLauncher.await();
  }

  private class MyLauncher extends Launcher {

    Async startAsync;

    MyLauncher(TestContext context) {
      startAsync = context.async();
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
      ExternalConfigurationTest.this.vertx = vertx;
      startAsync.countDown();
    }

    void await() {
      startAsync.await(3000);
    }
  }
}
