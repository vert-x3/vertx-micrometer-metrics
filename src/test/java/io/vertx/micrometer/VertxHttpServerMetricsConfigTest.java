/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.micrometer;

import io.micrometer.core.instrument.Tag;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joel Takvorian
 */
@RunWith(VertxUnitRunner.class)
public class VertxHttpServerMetricsConfigTest extends MicrometerMetricsTestBase {

  private HttpServer httpServer;

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    return super.metricOptions()
      .setServerRequestTagsProvider(req -> {
        String user = req.headers().get("user");
        return Collections.singletonList(Tag.of("user", user));
      });
  }

  @Test
  public void shouldReportHttpServerMetricsWithCustomTags(TestContext ctx) {
    vertx = vertx(ctx);

    prepareServer(ctx);
    HttpClient client = vertx.createHttpClient();
    sendRequest(ctx, client, "alice");
    sendRequest(ctx, client, "bob");

    waitForValue(ctx, "vertx.http.client.response.time[code=200,method=POST]$COUNT",
      value -> value.intValue() == 2);
    waitForValue(ctx, "vertx.http.client.active.requests[method=POST]$VALUE",
      value -> value.intValue() == 0);

    List<Datapoint> datapoints = listDatapoints(startsWith("vertx.http.server."));
    assertThat(datapoints).extracting(Datapoint::id).contains(
      "vertx.http.server.requests[code=200,method=POST,user=alice]$COUNT",
      "vertx.http.server.active.requests[method=POST,user=alice]$VALUE",
      "vertx.http.server.response.time[code=200,method=POST,user=alice]$COUNT",
      "vertx.http.server.requests[code=200,method=POST,user=bob]$COUNT",
      "vertx.http.server.active.requests[method=POST,user=bob]$VALUE",
      "vertx.http.server.response.time[code=200,method=POST,user=bob]$COUNT");
  }

  private void prepareServer(TestContext ctx) {
    // Setup server
    Async serverReady = ctx.async();
    vertx.deployVerticle(new AbstractVerticle() {
      @Override
      public void start(Promise<Void> startPromise) {
        httpServer = vertx.createHttpServer();
        httpServer
          .requestHandler(req -> {
            vertx.setTimer(30L, handler ->
              req.response().setChunked(true).putHeader("Content-Type", "text/plain").end(""));
          })
          .listen(9195, "127.0.0.1")
          .<Void>mapEmpty()
          .onComplete(startPromise);
      }
    }).onComplete(ctx.asyncAssertSuccess(v -> serverReady.complete()));
    serverReady.awaitSuccess();
  }

  private void sendRequest(TestContext ctx, HttpClient client, String user) {
    Async async = ctx.async();
    client.request(HttpMethod.POST, 9195, "127.0.0.1", "/")
      .compose(req -> req
        .putHeader("user", user)
        .send("")
        .compose(response -> {
          if (response.statusCode() != 200) {
            return Future.failedFuture(response.statusMessage());
          } else {
            return response.body();
          }
        }))
      .onComplete(ctx.asyncAssertSuccess(v -> async.countDown()));
    async.await();
  }
}
