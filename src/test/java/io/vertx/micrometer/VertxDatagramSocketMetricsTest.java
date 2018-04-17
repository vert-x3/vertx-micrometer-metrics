package io.vertx.micrometer;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static io.vertx.micrometer.RegistryInspector.*;
import static io.vertx.micrometer.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxDatagramSocketMetricsTest {

  @After
  public void teardown() {
    BackendRegistries.stop(MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME);
  }

  @Test
  public void shouldReportDatagramMetrics(TestContext context) throws InterruptedException {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
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

    waitForValue(vertx, context, "vertx.datagram.bytesSent[]$COUNT", value -> value.intValue() == 5);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.datagram."));
    assertThat(datapoints).containsOnly(
      dp("vertx.datagram.bytesSent[]$COUNT", 5),
      dp("vertx.datagram.bytesSent[]$TOTAL", 45),  // 45 = size("some text") * loops
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$COUNT", 5),
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$TOTAL", 45));
  }
}
