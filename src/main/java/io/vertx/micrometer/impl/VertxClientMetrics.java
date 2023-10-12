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
import io.micrometer.core.instrument.Timer;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.NAMESPACE;
import static io.vertx.micrometer.Label.REMOTE;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;

/**
 * @author Joel Takvorian
 */
class VertxClientMetrics extends AbstractMetrics implements ClientMetrics<Timer.Sample, Timer.Sample, Object, Object> {

  final Timer queueDelay;
  final LongAdder queueSize;
  final Timer processingTime;
  final LongAdder processingPending;
  final Counter resetCount;

  VertxClientMetrics(AbstractMetrics parent, SocketAddress remoteAddress, String type, String namespace) {
    super(parent, type);
    TagsWrapper tags = of(toTag(REMOTE, Labels::address, remoteAddress), toTag(NAMESPACE, s -> s == null ? "" : s, namespace));
    queueDelay = timer(names.getClientQueueTime(), "Time spent in queue before being processed", tags.unwrap());
    queueSize = longGauge(names.getClientQueuePending(), "Number of pending elements in queue", tags.unwrap());
    processingTime = timer(names.getClientProcessingTime(), "Processing time, from request start to response end", tags.unwrap());
    processingPending = longGauge(names.getClientProcessingPending(), "Number of elements being processed", tags.unwrap());
    resetCount = counter(names.getClientResetsCount(), "Total number of resets", tags.unwrap());
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
