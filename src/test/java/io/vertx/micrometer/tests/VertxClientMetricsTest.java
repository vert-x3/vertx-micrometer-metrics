/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.micrometer.tests;

import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Stack;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxClientMetricsTest extends MicrometerMetricsTestBase {

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions().addLabels(Label.REMOTE, Label.NAMESPACE);
  }

  @Test
  public void shouldReportProcessedClientMetrics(TestContext context) {
    vertx = vertx(context);

    FakeClient client = new FakeClient(vertx, "somewhere", "my namespace");

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.fake"));
    assertThat(datapoints).size().isEqualTo(5);

    client.process(6);
    datapoints = listDatapoints(startsWith("vertx.fake"));
    assertThat(datapoints).contains(
      dp("vertx.fake.processing.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 6));

    client.processed(2);
    datapoints = listDatapoints(startsWith("vertx.fake"));
    assertThat(datapoints).contains(
      dp("vertx.fake.processing.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 4),
      dp("vertx.fake.processing.time[client_namespace=my namespace,remote=somewhere]$COUNT", 2));

    client.reset(2);
    datapoints = listDatapoints(startsWith("vertx.fake"));
    assertThat(datapoints).contains(
      dp("vertx.fake.processing.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 2),
      dp("vertx.fake.processing.time[client_namespace=my namespace,remote=somewhere]$COUNT", 4),
      dp("vertx.fake.resets[client_namespace=my namespace,remote=somewhere]$COUNT", 2));
  }

  @Test
  public void shouldNotReportDisabledClientMetrics(TestContext context) {
    metricsOptions.addDisabledMetricsCategory("fake");

    vertx = vertx(context);

    FakeClient client = new FakeClient(vertx, "somewhere", "my namespace");
    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.fake"));
    assertThat(datapoints).isEmpty();
  }

  static class FakeClient {
    final Vertx vertx;
    final ClientMetrics metrics;
    final Stack<Object> queue = new Stack<>();
    final Stack<Object> processing = new Stack<>();

    FakeClient(Vertx vertx, String where, String namespace) {
      this.vertx = vertx;
      metrics = ((VertxInternal)vertx).metricsSPI().createClientMetrics(
        SocketAddress.domainSocketAddress(where), "fake", namespace);
    }

    void process(int quantity) {
      for (int i = 0; i < quantity; i++) {
        Object o = metrics.requestBegin("", "");
        metrics.requestEnd(o);
        processing.push(o);
      }
    }

    void processed(int quantity) {
      for (int i = 0; i < quantity; i++) {
        Object o = processing.pop();
        metrics.responseBegin(o, null);
        metrics.responseEnd(o);
      }
    }

    void reset(int quantity) {
      for (int i = 0; i < quantity; i++) {
        Object o = processing.pop();
        metrics.requestReset(o);
      }
    }
  }
}
