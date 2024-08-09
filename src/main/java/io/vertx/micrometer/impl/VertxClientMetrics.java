/*
 * Copyright (c) 2011-2023 The original author or authors
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.micrometer.impl.tags.Labels;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.NAMESPACE;
import static io.vertx.micrometer.Label.REMOTE;

/**
 * @author Joel Takvorian
 */
class VertxClientMetrics extends AbstractMetrics implements ClientMetrics<Sample, Object, Object> {

  final Timer processingTime;
  final LongAdder processingPending;
  final Counter resetCount;

  VertxClientMetrics(AbstractMetrics parent, SocketAddress remoteAddress, String type, String namespace) {
    super(parent, type);
    Tags tags = Tags.empty();
    if (enabledLabels.contains(REMOTE)) {
      tags = tags.and(REMOTE.toString(), Labels.address(remoteAddress));
    }
    if (enabledLabels.contains(NAMESPACE)) {
      tags = tags.and(NAMESPACE.toString(), namespace == null ? "" : namespace);
    }
    processingTime = Timer.builder(names.getClientProcessingTime())
      .description("Processing time, from request start to response end")
      .tags(tags)
      .register(registry);
    processingPending = longGaugeBuilder(names.getClientProcessingPending(), LongAdder::doubleValue)
      .description("Number of elements being processed")
      .tags(tags)
      .register(registry);
    resetCount = Counter.builder(names.getClientResetsCount())
      .description("Total number of resets")
      .tags(tags)
      .register(registry);
  }

  @Override
  public Sample requestBegin(String uri, Object request) {
    // Ignore parameters at the moment; need to carefully figure out what can be labelled or not
    processingPending.increment();
    return Timer.start();
  }

  @Override
  public void requestEnd(Sample requestMetric) {
    // Ignoring request-alone metrics at the moment
  }

  @Override
  public void responseBegin(Sample requestMetric, Object response) {
    // Ignoring response-alone metrics at the moment
  }

  @Override
  public void requestReset(Sample requestMetric) {
    processingPending.decrement();
    requestMetric.stop(processingTime);
    resetCount.increment();
  }

  @Override
  public void responseEnd(Sample requestMetric) {
    processingPending.decrement();
    requestMetric.stop(processingTime);
  }
}
