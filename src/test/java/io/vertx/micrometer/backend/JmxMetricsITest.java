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

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.MicrometerMetricsTestBase;
import io.vertx.micrometer.VertxJmxMetricsOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(VertxUnitRunner.class)
public class JmxMetricsITest extends MicrometerMetricsTestBase {

  @Test
  public void shouldReportJmx(TestContext context) throws Exception {
    metricsOptions = new MicrometerMetricsOptions()
      .setRegistryName(registryName)
      .addLabels(Label.EB_ADDRESS)
      .setJmxMetricsOptions(new VertxJmxMetricsOptions().setEnabled(true)
        .setDomain("my-metrics")
        .setStep(1))
      .setEnabled(true);

    vertx = vertx(context);

    // Send something on the eventbus and wait til it's received
    Async asyncEB = context.async();
    vertx.eventBus().consumer("test-eb", msg -> asyncEB.complete());
    vertx.eventBus().publish("test-eb", "test message");
    asyncEB.await(2000);

    // Read MBean
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    assertThat(mbs.getDomains()).contains("my-metrics");
    Hashtable<String, String> table = new Hashtable<>();
    table.put("type", "gauges");
    table.put("name", "vertxEventbusHandlers.address.test-eb");
    Number result = (Number) mbs.getAttribute(new ObjectName("my-metrics", table), "Value");
    assertThat(result).isEqualTo(1d);
  }
}
