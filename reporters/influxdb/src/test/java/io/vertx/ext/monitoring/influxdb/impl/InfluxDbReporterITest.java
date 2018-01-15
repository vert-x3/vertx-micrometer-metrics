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

package io.vertx.ext.monitoring.influxdb.impl;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.monitoring.influxdb.AuthenticationOptions;
import io.vertx.ext.monitoring.influxdb.VertxInfluxDbOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class InfluxDbReporterITest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxInfluxDbOptions().setAuthenticationOptions(
        new AuthenticationOptions().setUsername("xx").setSecret("yy").setEnabled(true))
        // Set batch delay in order to prevent multiple batch requests during test which leads to completing async twice
        .setBatchDelay(10)
        .setEnabled(true))
    );
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
        req.exceptionHandler(t -> System.out.println("An exception occured handling request: " + t));
        Buffer fullRequestBody = Buffer.buffer();
        req.handler(fullRequestBody::appendBuffer);
        req.endHandler(h -> {
          context.assertTrue(fullRequestBody.toString().contains("eventbus.publishedRemoteMessages=0i"));
          req.response().setStatusCode(200).end();
          async.complete();
        });
      }).listen(8086, "localhost", context.asyncAssertSuccess());
    async.awaitSuccess();
  }
}
