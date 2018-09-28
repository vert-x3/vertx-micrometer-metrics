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

  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportDatagramMetrics(TestContext context) throws InterruptedException {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .addLabels(Label.LOCAL)
      .setEnabled(true)))
      .exceptionHandler(context.exceptionHandler());

    String datagramContent = "some text";
    int loops = 5;

    // Setup server
    int port = 9192;
    String host = "localhost";
    Async receiveLatch = context.async(loops);
    Async listenLatch = context.async();
    vertx.createDatagramSocket().listen(port, host, context.asyncAssertSuccess(so -> {
      so.handler(packet -> receiveLatch.countDown());
      listenLatch.complete();
    }));
    listenLatch.awaitSuccess(15000);

    // Send to server
    DatagramSocket client = vertx.createDatagramSocket();
    for (int i = 0; i < loops; i++) {
      client.send(datagramContent, port, host, context.asyncAssertSuccess());
    }
    receiveLatch.awaitSuccess(15000);

    waitForValue(vertx, context, "vertx.datagram.bytesSent[]$COUNT", value -> value.intValue() == 5);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.datagram."));
    assertThat(datapoints).containsOnly(
      dp("vertx.datagram.bytesSent[]$COUNT", 5),
      dp("vertx.datagram.bytesSent[]$TOTAL", 45),  // 45 = size("some text") * loops
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$COUNT", 5),
      dp("vertx.datagram.bytesReceived[local=localhost:9192]$TOTAL", 45));
  }
}
