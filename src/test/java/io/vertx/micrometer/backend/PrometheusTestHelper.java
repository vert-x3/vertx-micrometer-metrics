/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package io.vertx.micrometer.backend;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Joel Takvorian
 */
public final class PrometheusTestHelper {
  private static final int DEFAULT_MAX_ATTEMPS = 10;
  private static final long DEFAULT_SLEEP_BEFORE_RETRY_MS = 100;

  private PrometheusTestHelper() {
  }

  public static void tryConnect(Vertx vertx, TestContext context, int port, String host, String requestURI, Handler<Buffer> bodyReader) {
    tryConnect(vertx, context, port, host, requestURI, bodyReader, DEFAULT_MAX_ATTEMPS, DEFAULT_SLEEP_BEFORE_RETRY_MS);
  }

  public static void tryConnect(Vertx vertx,
                                TestContext context,
                                int port,
                                String host,
                                String requestURI,
                                Handler<Buffer> bodyReader,
                                int maxAttempts,
                                long sleepBeforeRetryMs) {
    tryConnect(vertx, context, port, host, requestURI, res -> {
      context.assertEquals(200, res.statusCode());
      res.bodyHandler(bodyReader);
    }, maxAttempts, sleepBeforeRetryMs, 0);
  }

  public static Set<String> getMetricNames(Vertx vertx, TestContext context, int port, String host, String requestURI, long timeout) {
    return getMetricNames(vertx, context, port, host, requestURI, timeout, DEFAULT_MAX_ATTEMPS, DEFAULT_SLEEP_BEFORE_RETRY_MS);
  }

  public static Set<String> getMetricNames(Vertx vertx,
                                           TestContext context,
                                           int port,
                                           String host,
                                           String requestURI,
                                           long timeout,
                                           int maxAttempts,
                                           long sleepBeforeRetryMs) {
    Async async = context.async();
    Set<String> metrics = Collections.synchronizedSet(new HashSet<>());
    tryConnect(vertx, context, port, host, requestURI, resp -> {
      context.assertEquals(200, resp.statusCode());
      RecordParser parser = RecordParser.newDelimited("\n", resp);
      parser.exceptionHandler(context::fail).endHandler(v -> {
        async.countDown();
      }).handler(buffer -> {
        String line = buffer.toString();
        if (line.startsWith("# TYPE")) {
          metrics.add(line.split(" ")[2]);
        }
      });
    }, maxAttempts, sleepBeforeRetryMs, 0);
    async.await(timeout);
    return metrics;
  }

  private static void tryConnect(Vertx vertx,
                                 TestContext context,
                                 int port,
                                 String host,
                                 String requestURI,
                                 Handler<HttpClientResponse> respHandler,
                                 int maxAttempts,
                                 long sleepBeforeRetryMs,
                                 int attempt) {
    vertx.createHttpClient()
      .get(port, host, requestURI, ar -> {
        if (ar.succeeded()) {
          respHandler.handle(ar.result());
        } else {
          if (attempt < maxAttempts) {
            vertx.setTimer(sleepBeforeRetryMs, l -> {
              tryConnect(vertx, context, port, host, requestURI, respHandler, maxAttempts, sleepBeforeRetryMs, attempt + 1);
            });
          } else {
            context.fail(ar.cause());
          }
        }
      });
  }
}
