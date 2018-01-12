package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.DummyVertxMetrics;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class DatagramSocketTest {

  private Object watcherRef;

  @After
  public void teardown() {
    if (watcherRef != null) {
      DummyVertxMetrics.REPORTER.remove(watcherRef);
    }
  }

  @Test
  public void shouldReportDatagramMetrics(TestContext context) throws InterruptedException {
    Async async = context.async();
    String datagramContent = "some text";
    int loops = 5;
    watcherRef = DummyVertxMetrics.REPORTER.watch(name -> name.startsWith("vertx.datagram"), dataPoints -> {
      if (dataPoints.size() == 1) {
        // Not sent yet, but empty errorCount already reported
        return;
      }
      context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
        .containsOnly(
          tuple("vertx.datagram.localhost:9192.bytesSent", 45L),  // 45 = size("some text") * loops
          tuple("vertx.datagram.localhost:9192.bytesReceived", 45L), // same
          tuple("vertx.datagram.errorCount", 0L)));
      async.complete();
    });

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions().setEnabled(true)));
    // Setup server
    int port = 9192;
    String host = "localhost";
    vertx.createDatagramSocket().listen(port, host, res -> {
    });

    // Send to server
    DatagramSocket client = vertx.createDatagramSocket();
    for (int i = 0; i < loops; i++) {
      client.send(datagramContent, port, host, context.asyncAssertSuccess());
    }
  }
}
