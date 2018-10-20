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
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;

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
      vertx.close(context.asyncAssertSuccess());
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

    Set<String> metrics = getMetricNames(context, prometheusOptions);
    assertTrue(metrics.toString(), metrics.stream().anyMatch(s -> s.startsWith("vertx_http_client_")));
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

    Set<String> metrics = getMetricNames(context, prometheusOptions);

    Stream<String> expectedMetrics = Stream.<String>builder()
      .add("jvm_classes_loaded") // from classloader metrics
      .add("jvm_buffer_count") // from JVM memory metrics
      .add("system_cpu_count") // from processor metrics
      .add("jvm_threads_live") // from JVM thread metrics
      .build();
    assertTrue(metrics.toString(), metrics.containsAll(expectedMetrics.collect(toList())));
  }

  private Set<String> getMetricNames(TestContext context, VertxPrometheusOptions prometheusOptions) {
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
    return metrics;
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
