package io.vertx.monitoring;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import org.assertj.core.util.DoubleComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static io.vertx.monitoring.RegistryInspector.dp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxEventBusMetricsTest {

  @Test
  public void shouldReportEventbusMetrics(TestContext context) throws InterruptedException {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new VertxMonitoringOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
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
      public void start(Future<Void> future) throws Exception {
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

    List<RegistryInspector.Datapoint> datapoints = RegistryInspector.listWithoutTimers("vertx.eventbus.");
    assertThat(datapoints).containsOnly(
      dp("vertx.eventbus.handlers[address=testSubject]$Value", instances),
      dp("vertx.eventbus.pending[address=no handler,side=local]$Value", 0),
      dp("vertx.eventbus.pending[address=testSubject,side=local]$Value", 0),
      dp("vertx.eventbus.published[address=no handler,side=local]$Count", 2),
      dp("vertx.eventbus.published[address=testSubject,side=local]$Count", 8),
      dp("vertx.eventbus.received[address=no handler,side=local]$Count", 2),
      dp("vertx.eventbus.received[address=testSubject,side=local]$Count", 8),
      dp("vertx.eventbus.delivered[address=testSubject,side=local]$Count", 8),
      dp("vertx.eventbus.replyFailures[address=no handler,failure=NO_HANDLERS]$Count", 2),
      dp("vertx.eventbus.errors[address=testSubject,class=RuntimeException]$Count", 2 * instances));

    List<RegistryInspector.Datapoint> timersDp = RegistryInspector.listTimers("vertx.eventbus.");
    assertThat(timersDp)
      .usingFieldByFieldElementComparator()
      .usingComparatorForElementFieldsWithType(new DoubleComparator(0.1), Double.class)
      .containsOnly(
        dp("vertx.eventbus.processingTime[address=testSubject]$TotalTime", 180d * instances / 1000d),
        dp("vertx.eventbus.processingTime[address=testSubject]$Count", 8d * instances),
        dp("vertx.eventbus.processingTime[address=testSubject]$Max", 30d / 1000d));
  }
}
