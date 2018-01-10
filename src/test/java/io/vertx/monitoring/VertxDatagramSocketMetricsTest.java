package io.vertx.monitoring;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxDatagramSocketMetricsTest {
  @Test
  public void shouldReportDatagramMetrics(TestContext context) throws InterruptedException {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setEnabled(true)))
      .exceptionHandler(context.exceptionHandler());

    String datagramContent = "some text";
    int loops = 5;

    // Setup server
    int port = 9192;
    String host = "localhost";
    Async async = context.async(loops);
    vertx.createDatagramSocket().listen(port, host, res -> {
      res.result().handler(packet -> async.countDown());
    });

    // Send to server
    DatagramSocket client = vertx.createDatagramSocket();
    for (int i = 0; i < loops; i++) {
      client.send(datagramContent, port, host, context.asyncAssertSuccess());
    }
    async.awaitSuccess();

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.datagram.");
    assertThat(datapoints).containsOnly(
      dp("vertx.datagram.bytesSent[]$Count", 5),
      dp("vertx.datagram.bytesSent[]$Total", 45),  // 45 = size("some text") * loops
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$Count", 5),
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$Total", 45));
  }
}
