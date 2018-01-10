/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.monitoring.backend;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.monitoring.VertxMonitoringOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class InfluxDbReporterITest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new VertxMonitoringOptions()
        .setInfluxDbOptions(new VertxInfluxDbOptions().setEnabled(true)
          .setStep(1)
          .setEnabled(true))
        .setRegistryName("influx")
        .setEnabled(true)));
  }

  @After
  public void after(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldSendDataToInfluxDb(TestContext context) {
    Async async = context.async();
    vertx.createHttpServer(new HttpServerOptions()
      .setCompressionSupported(true)
      .setDecompressionSupported(true)
      .setLogActivity(true)
      .setHost("localhost")
      .setPort(8086))
      .requestHandler(req -> {
        req.exceptionHandler(context.exceptionHandler());
        Buffer fullRequestBody = Buffer.buffer();
        req.handler(fullRequestBody::appendBuffer);
        req.endHandler(h -> {
          String str = fullRequestBody.toString();
          if (str.isEmpty()) {
            req.response().setStatusCode(200).end();
            return;
          }
          try {
            context.verify(v -> assertThat(str)
              .contains("vertx_http_server_connections,local=localhost:8086,remote=_,metric_type=gauge value=1"));
          } finally {
            req.response().setStatusCode(200).end();
            async.complete();
          }
        });
      }).listen(8086, "localhost", context.asyncAssertSuccess());
    async.awaitSuccess();
  }
}
