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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxClientMetrics extends AbstractMetrics {
  private final Timers queueDelay;
  private final Gauges<LongAdder> queueSize;
  private final Timers processingTime;
  private final Gauges<LongAdder> processingPending;
  private final Counters resetCount;

  VertxClientMetrics(MeterRegistry registry, String type) {
    super(registry, type);
    queueDelay = timers("queue.time", "Time spent in queue before being processed", Label.REMOTE, Label.NAMESPACE);
    queueSize = longGauges("queue.pending", "Number of pending elements in queue", Label.REMOTE, Label.NAMESPACE);
    processingTime = timers("processing.time", "Processing time, from request start to response end", Label.REMOTE, Label.NAMESPACE);
    processingPending = longGauges("processing.pending", "Number of elements being processed", Label.REMOTE, Label.NAMESPACE);
    resetCount = counters("reset", "Total number of requests reset", Label.REMOTE, Label.NAMESPACE);
  }

  ClientMetrics forInstance(SocketAddress remoteAddress, String namespace) {
    return new Instance(remoteAddress, namespace);
  }

  class Instance implements ClientMetrics<Timers.EventTiming, Timers.EventTiming, Object, Object> {
    private final String remote;
    private final String namespace;

    Instance(SocketAddress remoteAddress, String namespace) {
      this.remote = remoteAddress == null ? "" : remoteAddress.toString();
      this.namespace = namespace == null ? "" : namespace;
    }

    @Override
    public Timers.EventTiming enqueueRequest() {
      queueSize.get(remote, namespace).increment();
      return queueDelay.start();
    }

    @Override
    public void dequeueRequest(Timers.EventTiming taskMetric) {
      queueSize.get(remote, namespace).decrement();
      taskMetric.end(remote, namespace);
    }

    @Override
    public Timers.EventTiming requestBegin(String uri, Object request) {
      // Ignore parameters at the moment; need to carefully figure out what can be labelled or not
      processingPending.get(remote, namespace).increment();
      return processingTime.start();
    }

    @Override
    public void requestEnd(Timers.EventTiming requestMetric) {
      // Ignoring request-alone metrics at the moment
    }

    @Override
    public void responseBegin(Timers.EventTiming requestMetric, Object response) {
      // Ignoring response-alone metrics at the moment
    }

    @Override
    public void requestReset(Timers.EventTiming requestMetric) {
      processingPending.get(remote, namespace).decrement();
      requestMetric.end(remote, namespace);
      resetCount.get(remote, namespace).increment();
    }

    @Override
    public void responseEnd(Timers.EventTiming requestMetric) {
      processingPending.get(remote, namespace).decrement();
      requestMetric.end(remote, namespace);
    }

    @Override
    public void close() {
    }
  }
}
