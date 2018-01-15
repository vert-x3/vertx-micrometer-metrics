package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.Comparators;
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
public class EventBusTest {

  private Object watcherRef;

  @After
  public void teardown() {
    if (watcherRef != null) {
      DummyVertxMetrics.REPORTER.remove(watcherRef);
    }
  }

  @Test
  public void shouldReportEventbusMetrics(TestContext context) throws InterruptedException {
    int instances = 2;
    Async assertions = context.async();
    watcherRef = DummyVertxMetrics.REPORTER.watch(
      name -> name.startsWith("vertx.eventbus"),  // filter
      dp -> dp.getName().equals("vertx.eventbus.handlers") && (double) (dp.getValue()) > 0, // wait until
      dataPoints -> {
        context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
          // We use a special comparator for processingTime: must be >= expected and <= expected+100ms
          .usingElementComparator(Comparators.metricValueComparator("vertx.eventbus.testSubject.processingTime",
            (actual, expected) ->
              (actual.longValue() >= expected.longValue() && actual.longValue() <= expected.longValue() + 100 * instances)
                ? 0 : -1))
          .containsOnly(
            tuple("vertx.eventbus.handlers", 1.0 * instances),
            tuple("vertx.eventbus.testSubject.processingTime", 180L * instances),
            tuple("vertx.eventbus.errorCount", 2L * instances),
            tuple("vertx.eventbus.bytesWritten", 0L),
            tuple("vertx.eventbus.bytesRead", 0L),
            tuple("vertx.eventbus.pending", 0.0),
            tuple("vertx.eventbus.pendingLocal", 0.0),
            tuple("vertx.eventbus.pendingRemote", 0.0),
            tuple("vertx.eventbus.publishedMessages", 10L),
            tuple("vertx.eventbus.publishedLocalMessages", 10L),
            tuple("vertx.eventbus.publishedRemoteMessages", 0L),
            tuple("vertx.eventbus.sentMessages", 0L),
            tuple("vertx.eventbus.sentLocalMessages", 0L),
            tuple("vertx.eventbus.sentRemoteMessages", 0L),
            tuple("vertx.eventbus.receivedMessages", 10L),
            tuple("vertx.eventbus.receivedLocalMessages", 10L),
            tuple("vertx.eventbus.receivedRemoteMessages", 0L),
            tuple("vertx.eventbus.deliveredMessages", 8L),
            tuple("vertx.eventbus.deliveredLocalMessages", 8L),
            tuple("vertx.eventbus.deliveredRemoteMessages", 0L),
            tuple("vertx.eventbus.replyFailures", 2L)));
        assertions.complete();
      },
      context::fail);

    Async ebReady = context.async(instances);
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions().setEnabled(true)));
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
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
  }
}
