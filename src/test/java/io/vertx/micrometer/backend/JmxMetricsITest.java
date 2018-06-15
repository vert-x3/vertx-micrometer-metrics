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
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxJmxMetricsOptions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class JmxMetricsITest {

  private static final String REGISTRY_NAME = "jmx-test";
  private Vertx vertx;

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldReportJmx(TestContext context) throws Exception {
    vertx = Vertx.vertx(new VertxOptions()
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setRegistryName(REGISTRY_NAME)
        .addLabels(Label.EB_ADDRESS)
        .setJmxMetricsOptions(new VertxJmxMetricsOptions().setEnabled(true)
          .setDomain("my-metrics")
          .setStep(1))
        .setEnabled(true)));

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Read MBean
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    assertThat(mbs.getDomains()).contains("my-metrics");
    Number result = (Number) mbs.getAttribute(new ObjectName("my-metrics", "name", "vertxEventbusHandlers.address.test-eb"), "Value");
    assertThat(result).isEqualTo(1d);
  }
}
