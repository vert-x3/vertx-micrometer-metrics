package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.DummyVertxMetrics;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.groups.Tuple;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class EventBusTest {

  private Object observerRef;

  @After
  public void teardown() {
    if (observerRef != null) {
      DummyVertxMetrics.REPORTER.remove(observerRef);
    }
  }

  @Test
  public void shouldReportEventbusMetrics(TestContext context) throws InterruptedException {
    Async async = context.async();
    observerRef = DummyVertxMetrics.REPORTER.observe(name -> name.startsWith("vertx.eventbus"), dataPoints -> {
      context.verify(v -> assertThat(dataPoints).extracting(DataPoint::getName, DataPoint::getValue)
        // We use a special comparator for processingTime: must be >= expected and <= expected+100ms
        .usingElementComparator(metricValueComparator("vertx.eventbus.testSubject.processingTime",
          (actual, expected) -> (actual.longValue() >= expected.longValue() && actual.longValue() <= expected.longValue() + 100) ? 0 : -1))
        .containsOnly(
          tuple("vertx.eventbus.handlers", 1.0),
          tuple("vertx.eventbus.testSubject.processingTime", 180L),
          tuple("vertx.eventbus.errorCount", 3L),
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
      async.complete();
    });

    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(new BatchingReporterOptions().setEnabled(true)));
    // Setup eventbus handler
    vertx.eventBus().consumer("testSubject", msg -> {
      JsonObject body = (JsonObject) msg.body();
      try {
        Thread.sleep(body.getLong("sleep"));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (body.getBoolean("fail")) {
        throw new RuntimeException("expected failure");
      }
    });

    // Send to eventbus
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": true, \"sleep\": 10}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": true, \"sleep\": 10}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": true, \"sleep\": 10}"));
    vertx.eventBus().publish("testSubject", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    vertx.eventBus().publish("no handler", new JsonObject("{\"fail\": false, \"sleep\": 30}"));
    async.awaitSuccess();
  }

  private static Comparator<Tuple> metricValueComparator(String metricName, Comparator<Number> comparator) {
    return metricValueComparators(Collections.singletonMap(metricName, comparator));
  }

  private static Comparator<Tuple> metricValueComparators(Map<String, Comparator<Number>> specialComparators) {
    return (t1, t2) -> {
      Object[] arr1 = t1.toArray();
      Object[] arr2 = t2.toArray();
      String name1 = (String) arr1[0];
      String name2 = (String) arr2[0];
      if (name1.equals(name2)) {
        Number num1 = (Number) arr1[1];
        Number num2 = (Number) arr2[1];
        if (specialComparators.containsKey(name1)) {
          return specialComparators.get(name1).compare(num1, num2);
        } else {
          return num1.equals(num2) ? 0 : -1;
        }
      }
      return -1;
    };
  }
}
