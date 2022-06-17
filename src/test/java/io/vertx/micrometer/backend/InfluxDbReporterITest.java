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

package io.vertx.micrometer.backend;


import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.MicrometerMetricsTestBase;
import io.vertx.micrometer.VertxInfluxDbOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class InfluxDbReporterITest extends MicrometerMetricsTestBase {

  private Vertx vertxForSimulatedServer = Vertx.vertx();

  @Override
  protected void tearDown(TestContext context) {
    super.tearDown(context);
    vertxForSimulatedServer.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldSendDataToInfluxDb(TestContext context) throws Exception {
    // Mock an influxdb server
    Async asyncInflux = context.async();
    InfluxDbTestHelper.simulateInfluxServer(vertxForSimulatedServer, context, 8086, body -> {
      if (body.contains("vertx_eventbus_handlers,address=test-eb,metric_type=gauge value=1")) {
        asyncInflux.complete();
      }
    });

    metricsOptions = new MicrometerMetricsOptions()
      .setInfluxDbOptions(new VertxInfluxDbOptions()
        .setStep(1)
        .setDb("mydb")
        .setEnabled(true))
      .setRegistryName(registryName)
      .addLabels(Label.EB_ADDRESS)
      .setEnabled(true);

    vertx = vertx(context);

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Await influx
    asyncInflux.awaitSuccess(2000);
  }
}
