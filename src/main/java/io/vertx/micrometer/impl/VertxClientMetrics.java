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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.NAMESPACE;
import static io.vertx.micrometer.Label.REMOTE;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;

/**
 * @author Joel Takvorian
 */
class VertxClientMetrics extends AbstractMetrics {

  VertxClientMetrics(MeterRegistry registry, String type, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, type, longGauges, enabledLabels);
  }

  ClientMetrics<Timer.Sample, Timer.Sample, Object, Object> forInstance(SocketAddress remoteAddress, String namespace) {
    return new Instance(remoteAddress, namespace);
  }

  class Instance implements ClientMetrics<Timer.Sample, Timer.Sample, Object, Object> {

    final Timer queueDelay;
    final LongAdder queueSize;
    final Timer processingTime;
    final LongAdder processingPending;
    final Counter resetCount;


    Instance(SocketAddress remoteAddress, String namespace) {
      TagsWrapper tags = of(toTag(REMOTE, Labels::address, remoteAddress), toTag(NAMESPACE, s -> s == null ? "" : s, namespace));
      queueDelay = timer(names.getClientQueueTime())
        .description("Time spent in queue before being processed")
        .tags(tags.unwrap())
        .register(registry);
      queueSize = longGauge(names.getClientQueuePending())
        .description("Number of pending elements in queue")
        .tags(tags.unwrap())
        .register(registry);
      processingTime = timer(names.getClientProcessingTime())
        .description("Processing time, from request start to response end")
        .tags(tags.unwrap())
        .register(registry);
      processingPending = longGauge(names.getClientProcessingPending())
        .description("Number of elements being processed")
        .tags(tags.unwrap())
        .register(registry);
      resetCount = counter(names.getClientResetsCount())
        .description("Total number of resets")
        .tags(tags.unwrap())
        .register(registry);
    }

    @Override
    public Timer.Sample enqueueRequest() {
      queueSize.increment();
      return Timer.start();
    }

    @Override
    public void dequeueRequest(Timer.Sample taskMetric) {
      queueSize.decrement();
      taskMetric.stop(queueDelay);
    }

    @Override
    public Timer.Sample requestBegin(String uri, Object request) {
      // Ignore parameters at the moment; need to carefully figure out what can be labelled or not
      processingPending.increment();
      return Timer.start();
    }

    @Override
    public void requestEnd(Timer.Sample requestMetric) {
      // Ignoring request-alone metrics at the moment
    }

    @Override
    public void responseBegin(Timer.Sample requestMetric, Object response) {
      // Ignoring response-alone metrics at the moment
    }

    @Override
    public void requestReset(Timer.Sample requestMetric) {
      processingPending.decrement();
      requestMetric.stop(processingTime);
      resetCount.increment();
    }

    @Override
    public void responseEnd(Timer.Sample requestMetric) {
      processingPending.decrement();
      requestMetric.stop(processingTime);
    }
  }
}
