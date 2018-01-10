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

package io.vertx.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Verticle;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Gauges;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxVerticleMetrics extends AbstractMetrics {
  private final Gauges<LongAdder> verticles;

  VertxVerticleMetrics(LabelMatchers labelMatchers, MeterRegistry registry) {
    super(labelMatchers, registry, MetricsCategory.VERTICLES, "vertx.");
    verticles = longGauges("verticle", "Number of verticle instances deployed", "name");
  }

  void verticleDeployed(Verticle verticle) {
    verticles.get(labelMatchers, verticle.getClass().getName()).increment();
  }

  void verticleUndeployed(Verticle verticle) {
    verticles.get(labelMatchers, verticle.getClass().getName()).decrement();
  }
}
