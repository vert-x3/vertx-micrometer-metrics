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

package io.vertx.micrometer;

import io.vertx.core.datagram.DatagramSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxDatagramNetClientNetServerSocketMetricsTest extends MicrometerMetricsTestBase {

  @Test
  public void shouldReportDatagramMetrics(TestContext context) {
    metricsOptions.addLabels(Label.LOCAL);

    vertx = vertx(context);

    String datagramContent = "some text";
    int loops = 5;

    // Setup server
    int port = 9192;
    String host = "localhost";
    Async receiveLatch = context.async(loops);
    Async listenLatch = context.async();
    vertx.createDatagramSocket().listen(port, host).onComplete(context.asyncAssertSuccess(so -> {
      so.handler(packet -> receiveLatch.countDown());
      listenLatch.complete();
    }));
    listenLatch.awaitSuccess(15000);

    // Send to server
    DatagramSocket client = vertx.createDatagramSocket();
    for (int i = 0; i < loops; i++) {
      client.send(datagramContent, port, host).onComplete(context.asyncAssertSuccess());
    }
    receiveLatch.awaitSuccess(15000);

    waitForValue(context, "vertx.datagram.bytes.written[]$COUNT", value -> value.intValue() == 5);
    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.datagram."));
    assertThat(datapoints).containsOnly(
      dp("vertx.datagram.bytes.written[]$COUNT", 5),
      dp("vertx.datagram.bytes.written[]$TOTAL", 45),  // 45 = size("some text") * loops
      dp("vertx.datagram.bytes.read[local=localhost:9192]$COUNT", 5),
      dp("vertx.datagram.bytes.read[local=localhost:9192]$TOTAL", 45));
  }

  @Test
  public void shouldReportInCompatibilityMode(TestContext context) {
    metricsOptions.setMetricsNaming(MetricsNaming.v3Names());

    vertx = vertx(context);

    String datagramContent = "some text";

    // Setup server
    int port = 9192;
    String host = "localhost";
    Async receiveLatch = context.async();
    Async listenLatch = context.async();
    vertx.createDatagramSocket().listen(port, host).onComplete(context.asyncAssertSuccess(so -> {
      so.handler(packet -> receiveLatch.countDown());
      listenLatch.complete();
    }));
    listenLatch.awaitSuccess(15000);

    // Send to server
    DatagramSocket client = vertx.createDatagramSocket();
    client.send(datagramContent, port, host).onComplete(context.asyncAssertSuccess());
    receiveLatch.awaitSuccess(15000);

    waitForValue(context, "vertx.datagram.bytesSent[]$COUNT", value -> value.intValue() == 1);
    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.datagram."));
    assertThat(datapoints).containsOnly(
      dp("vertx.datagram.bytesSent[]$COUNT", 1),
      dp("vertx.datagram.bytesSent[]$TOTAL", 9),
      dp("vertx.datagram.bytesReceived[]$COUNT", 1),
      dp("vertx.datagram.bytesReceived[]$TOTAL", 9));
  }
}
