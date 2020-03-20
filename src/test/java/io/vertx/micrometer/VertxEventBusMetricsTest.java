package io.vertx.micrometer;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.util.DoubleComparator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static io.vertx.micrometer.RegistryInspector.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxEventBusMetricsTest {

  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportEventbusMetrics(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .addLabels(Label.EB_ADDRESS, Label.EB_FAILURE, Label.CLASS_NAME)
      .setEnabled(true)))
      .exceptionHandler(t -> {
        if (t.getMessage() == null || !t.getMessage().contains("expected failure")) {
          context.exceptionHandler().handle(t);
        }
      });

    int instances = 2;

    Async ebReady = context.async(instances);
    Async allReceived = context.async(instances);
    // Setup eventbus handler
    vertx.deployVerticle(() -> new AbstractVerticle() {
      @Override
      public void start(Promise<Void> future) {
        vertx.eventBus().consumer("testSubject", msg -> {
          JsonObject body = (JsonObject) msg.body();
          try {
            Thread.sleep(body.getLong("sleep"));
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          if (body.containsKey("last")) {
            allReceived.countDown();
          }
          if (body.getBoolean("fail")) {
            throw new RuntimeException("It's ok! [expected failure]");
          }
        });
        ebReady.countDown();
      }
    }, new DeploymentOptions().setInstances(instances));

    ebReady.awaitSuccess();
    // Send to eventbus
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 10}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": true, \"sleep\": 10}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": true, \"sleep\": 10}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30, \"last\": true}"));
    allReceived.awaitSuccess();

    waitForValue(vertx, context, "vertx.eventbus.processingTime[address=testSubject]$COUNT",
      value -> value.intValue() == 8 * instances);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.eventbus"));
    assertThat(datapoints).hasSize(13).contains(
      dp("vertx.eventbus.handlers[address=testSubject]$VALUE", instances),
      dp("vertx.eventbus.pending[address=no handler,side=local]$VALUE", 0),
      dp("vertx.eventbus.pending[address=testSubject,side=local]$VALUE", 0),
      dp("vertx.eventbus.published[address=no handler,side=local]$COUNT", 2),
      dp("vertx.eventbus.published[address=testSubject,side=local]$COUNT", 8),
      dp("vertx.eventbus.received[address=no handler,side=local]$COUNT", 2),
      dp("vertx.eventbus.received[address=testSubject,side=local]$COUNT", 8),
      dp("vertx.eventbus.delivered[address=testSubject,side=local]$COUNT", 8),
      dp("vertx.eventbus.replyFailures[address=no handler,failure=NO_HANDLERS]$COUNT", 2),
      dp("vertx.eventbus.errors[address=testSubject,class=RuntimeException]$COUNT", 2 * instances),
      dp("vertx.eventbus.processingTime[address=testSubject]$COUNT", 8d * instances));

    assertThat(datapoints)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(1.0), Double.class)
      .contains(
        dp("vertx.eventbus.processingTime[address=testSubject]$TOTAL_TIME", 180d * instances / 1000d),
        dp("vertx.eventbus.processingTime[address=testSubject]$MAX", 30d / 1000d));
  }

  @Test
  public void shouldDiscardMessages(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setEnabled(true)))
      .exceptionHandler(t -> context.exceptionHandler().handle(t));

    int num = 10;
    EventBus eb = vertx.eventBus();
    MessageConsumer<Object> consumer = eb.consumer("foo");
    consumer.setMaxBufferedMessages(num);
    consumer.pause();
    consumer.handler(msg -> fail("should not be called"));
    for (int i = 0; i < num; i++) {
      eb.send("foo", "the_message-" + i);
    }
    eb.send("foo", "last");

    waitForValue(vertx, context, "vertx.eventbus.discarded[side=local]$COUNT", value -> value.intValue() == 1);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.eventbus"));
    assertThat(datapoints).contains(dp("vertx.eventbus.pending[side=local]$VALUE", 10));

    // Unregister => discard all remaining
    consumer.unregister();
    waitForValue(vertx, context, "vertx.eventbus.discarded[side=local]$COUNT", value -> value.intValue() == 11);
    datapoints = listDatapoints(startsWith("vertx.eventbus"));
    assertThat(datapoints).contains(dp("vertx.eventbus.pending[side=local]$VALUE", 0));
  }
}
