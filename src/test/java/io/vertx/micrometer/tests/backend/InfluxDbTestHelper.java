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
package io.vertx.micrometer.tests.backend;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import java.util.function.Consumer;

/**
 * @author Joel Takvorian
 */
public final class InfluxDbTestHelper {
  private InfluxDbTestHelper() {
  }

  static void simulateInfluxServer(Vertx vertx, TestContext context, int port, Consumer<String> onRequest) {
    Async ready = context.async();
    vertx.runOnContext(v -> vertx.createHttpServer(new HttpServerOptions()
      .setCompressionSupported(true)
      .setDecompressionSupported(true)
      .setLogActivity(true)
      .setHost("localhost")
      .setPort(port))
      .requestHandler(req -> {
        req.exceptionHandler(context.exceptionHandler());
        req.bodyHandler(buffer -> {
          String str = buffer.toString();
          if (str.isEmpty()) {
            req.response().setStatusCode(200).end();
            return;
          }
          try {
            onRequest.accept(str);
          } finally {
            req.response().setStatusCode(200).end();
          }
        });
      })
      .exceptionHandler(System.err::println)
      .listen(port, "localhost").onComplete(res -> {
        if (res.succeeded()) {
          ready.complete();
        } else {
          context.fail(res.cause());
        }
      }));
    ready.await(10000);
  }
}
