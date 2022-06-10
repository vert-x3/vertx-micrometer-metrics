package io.vertx.micrometer;

import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
  public void shouldReportQueueClientMetrics(TestContext context) {
    vertx = vertx(context);

    FakeClient client = new FakeClient(vertx, "somewhere", "my namespace");

    List<Datapoint> datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).isEmpty();

    client.enqueue(10);
    datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).containsOnly(
      dp("vertx.fake.queue.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 10));

    client.dequeue(8);
    datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).contains(
      dp("vertx.fake.queue.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 2),
      dp("vertx.fake.queue.time[client_namespace=my namespace,remote=somewhere]$COUNT", 8));
  }

  @Test
  public void shouldReportProcessedClientMetrics(TestContext context) {
    vertx = vertx(context);

    FakeClient client = new FakeClient(vertx, "somewhere", "my namespace");

    List<Datapoint> datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).isEmpty();

    client.process(6);
    datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).containsOnly(
      dp("vertx.fake.processing.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 6));

    client.processed(2);
    datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
    assertThat(datapoints).contains(
      dp("vertx.fake.processing.pending[client_namespace=my namespace,remote=somewhere]$VALUE", 4),
      dp("vertx.fake.processing.time[client_namespace=my namespace,remote=somewhere]$COUNT", 2));

    client.reset(2);
    datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
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
    client.enqueue(4);
    client.dequeue(1);
    List<Datapoint> datapoints = listDatapoints(MicrometerMetricsTestBase.ALL);
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

    void enqueue(int quantity) {
      for (int i = 0; i < quantity; i++) {
        Object o = metrics.enqueueRequest();
        queue.push(o);
      }
    }

    void dequeue(int quantity) {
      for (int i = 0; i < quantity; i++) {
        Object o = queue.pop();
        metrics.dequeueRequest(o);
      }
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
