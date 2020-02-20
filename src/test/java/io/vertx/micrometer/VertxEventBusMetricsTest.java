package io.vertx.micrometer;

import io.vertx.core.*;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.vertx.micrometer.RegistryInspector.*;
import static org.assertj.core.api.Assertions.assertThat;

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
  public void shouldFlushPendingOnUnregister(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new MicrometerMetricsOptions()
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
      .setEnabled(true)))
      .exceptionHandler(t -> {
        if (t.getMessage() == null || !t.getMessage().contains("expected failure")) {
          context.exceptionHandler().handle(t);
        }
      });

    Async step1EBReady = context.async();
    Async step2FirstReceived = context.async();
    Async step3BlockWorker = context.async();
    AtomicBoolean first = new AtomicBoolean(true);
    AtomicReference<MessageConsumer<Object>> consumerRef = new AtomicReference<>();

    // Setup eventbus handler
    vertx.deployVerticle(() -> new AbstractVerticle() {
      @Override
      public void start(Promise<Void> future) {
        MessageConsumer<Object> consumer = vertx.eventBus().consumer("testSubject", msg -> {
          if (first.getAndSet(false)) {
            step2FirstReceived.complete();
          }
          step3BlockWorker.await();
        });
        consumerRef.set(consumer);
        step1EBReady.complete();
      }
    }, new DeploymentOptions());
    step1EBReady.awaitSuccess();

    // Send to eventbus
    vertx.eventBus().publish("testSubject", new JsonObject());
    vertx.eventBus().publish("testSubject", new JsonObject());
    vertx.eventBus().publish("testSubject", new JsonObject());
    vertx.eventBus().publish("testSubject", new JsonObject());

    // Check pending count, as first event should block the other ones => expect 3 pending
    step2FirstReceived.awaitSuccess();
    // RegistryInspector.dump(startsWith("vertx.eventbus"));
    waitForValue(vertx, context, "vertx.eventbus.published[side=local]$COUNT", value -> value.intValue() == 4);
    List<RegistryInspector.Datapoint> datapoints = listDatapoints(startsWith("vertx.eventbus"));
    assertThat(datapoints).contains(dp("vertx.eventbus.pending[side=local]$VALUE", 3));

    // Unregister handler, then check pending again => should be 0
    consumerRef.get().unregister();
    // RegistryInspector.dump(startsWith("vertx.eventbus"));
    waitForValue(vertx, context, "vertx.eventbus.handlers[]$VALUE", value -> value.intValue() == 0);
    datapoints = listDatapoints(startsWith("vertx.eventbus"));
    assertThat(datapoints).contains(dp("vertx.eventbus.pending[side=local]$VALUE", 0));

    // (Unblock thread)
    step3BlockWorker.complete();
  }
}
