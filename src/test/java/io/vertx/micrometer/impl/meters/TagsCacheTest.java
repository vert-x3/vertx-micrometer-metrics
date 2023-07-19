package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.Tags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ThreadLocalRandom;

@RunWith(VertxUnitRunner.class)
public class TagsCacheTest {

  private Vertx vertx = Vertx.vertx();

  @After
  public void tearDown(TestContext tc) throws Exception {
    vertx.close().onComplete(tc.asyncAssertSuccess());
  }

  @Test
  public void testCallsFromELThreadOnELContext(TestContext tc) {
    Promise<Void> promise = Promise.promise();
    Label[] labels = Label.values();
    vertx.runOnContext(v -> {
      Label label = labels[ThreadLocalRandom.current().nextInt(0, labels.length)];
      String value = String.valueOf(label.ordinal());
      Tags expectedTags = TagsCache.createTags(null, new Label[]{label}, new String[]{value});
      Tags first = TagsCache.getOrCreate(null, new Label[]{label}, new String[]{value});
      tc.assertEquals(expectedTags, first);
      Tags second = TagsCache.getOrCreate(null, new Label[]{label}, new String[]{value});
      tc.assertTrue(first == second);
      promise.complete();
    });
    promise.future().onComplete(tc.asyncAssertSuccess());
  }

  @Test
  public void testConcurrentCallsFromWorkerThreadsOnELContext(TestContext tc) {
    Async async = tc.async(10000);
    vertx.deployVerticle(new MyAbstractVerticle(tc, async)).onComplete(tc.asyncAssertSuccess());
    async.awaitSuccess();
  }

  private static class MyAbstractVerticle extends AbstractVerticle {

    final TestContext tc;
    final Async async;
    final int count;

    MyAbstractVerticle(TestContext tc, Async async) {
      this.tc = tc;
      this.async = async;
      count = async.count();
    }

    @Override
    public void start() throws Exception {
      Label[] labels = Label.values();
      for (int i = 0; i < count; i++) {
        vertx.executeBlocking(() -> {
          Label label = labels[ThreadLocalRandom.current().nextInt(0, labels.length)];
          String value = String.valueOf(label.ordinal());
          Tags expectedTags = TagsCache.createTags(null, new Label[]{label}, new String[]{value});
          Tags actual = TagsCache.getOrCreate(null, new Label[]{label}, new String[]{value});
          tc.assertEquals(expectedTags, actual);
          return null;
        }, false).onSuccess(v -> async.countDown());
      }
    }
  }
}
