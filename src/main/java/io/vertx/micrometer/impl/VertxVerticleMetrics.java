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

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Verticle;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Gauges;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxVerticleMetrics extends AbstractMetrics {
  private final Gauges<LongAdder> deployed;

  VertxVerticleMetrics(MeterRegistry registry, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.VERTICLES, gaugesTable);
    deployed = longGauges("deployed", "Number of verticle instances deployed", Label.CLASS_NAME);
  }

  void verticleDeployed(Verticle verticle) {
    deployed.get(verticle.getClass().getName()).increment();
  }

  void verticleUndeployed(Verticle verticle) {
    deployed.get(verticle.getClass().getName()).decrement();
  }
}
