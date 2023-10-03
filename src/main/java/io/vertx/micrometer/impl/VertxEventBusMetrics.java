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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static java.util.function.UnaryOperator.identity;

/**
 * @author Joel Takvorian
 */
class VertxEventBusMetrics extends AbstractMetrics implements EventBusMetrics<VertxEventBusMetrics.Handler> {

  VertxEventBusMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, MetricsDomain.EVENT_BUS, longGauges, enabledLabels);
  }

  private static boolean isNotInternal(String address) {
    return !address.startsWith("__vertx.");
  }

  @Override
  public Handler handlerRegistered(String address) {
    if (isNotInternal(address)) {
      Handler handler = new Handler(toTags(EB_ADDRESS, identity(), address));
      handler.handlers.increment();
      return handler;
    }
    return null;
  }

  @Override
  public void handlerUnregistered(Handler handler) {
    if (handler != null) {
      handler.handlers.decrement();
    }
  }

  @Override
  public void messageDelivered(Handler handler, boolean local) {
    if (handler != null) {
      Tags tags = handler.tags.and(toTags(EB_SIDE, Labels::side, local));
      longGauge(names.getEbPending())
        .description("Number of messages not processed yet")
        .tags(tags)
        .register(registry)
        .decrement();
      counter(names.getEbProcessed())
        .description("Number of processed messages")
        .tags(tags)
        .register(registry)
        .increment();
    }
  }

  @Override
  public void discardMessage(Handler handler, boolean local, Message<?> msg) {
    if (handler != null) {
      Tags tags = handler.tags.and(toTags(EB_SIDE, Labels::side, local));
      longGauge(names.getEbPending())
        .description("Number of messages not processed yet")
        .tags(tags)
        .register(registry)
        .decrement();
      counter(names.getEbDiscarded())
        .description("Number of discarded messages")
        .tags(tags)
        .register(registry)
        .increment();
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (isNotInternal(address)) {
      Tags tags = toTags(EB_ADDRESS, identity(), address, EB_SIDE, Labels::side, local);
      if (publish) {
        counter(names.getEbPublished())
          .description("Number of messages published (publish / subscribe)")
          .tags(tags)
          .register(registry)
          .increment();
      } else {
        counter(names.getEbSent())
          .description("Number of messages sent (point-to-point)")
          .tags(tags)
          .register(registry)
          .increment();
      }
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    if (isNotInternal(address)) {
      Tags tags = toTags(EB_ADDRESS, identity(), address, EB_SIDE, Labels::side, local);
      counter(names.getEbReceived())
        .description("Number of messages received")
        .tags(tags)
        .register(registry)
        .increment();
      if (handlers > 0) {
        longGauge(names.getEbPending())
          .description("Number of messages not processed yet")
          .tags(tags)
          .register(registry)
          .add(handlers);
        counter(names.getEbDelivered())
          .description("Number of messages delivered to handlers")
          .tags(tags)
          .register(registry)
          .increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      distributionSummary(names.getEbBytesWritten())
        .description("Number of bytes sent while sending messages to event bus cluster peers")
        .tags(toTags(EB_ADDRESS, identity(), address))
        .register(registry)
        .record(numberOfBytes);
    }
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      distributionSummary(names.getEbBytesRead())
        .description("Number of bytes received while reading messages from event bus cluster peers")
        .tags(toTags(EB_ADDRESS, identity(), address))
        .register(registry)
        .record(numberOfBytes);
    }
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    if (isNotInternal(address)) {
      counter(names.getEbReplyFailures())
        .description("Number of message reply failures")
        .tags(toTags(EB_ADDRESS, identity(), address, EB_FAILURE, ReplyFailure::name, failure))
        .register(registry).increment();
    }
  }

  class Handler {

    final Tags tags;
    final LongAdder handlers;

    Handler(Tags tags) {
      handlers = longGauge(names.getEbHandlers())
        .description("Number of event bus handlers in use")
        .tags(tags)
        .register(registry);
      this.tags = tags;
    }
  }
}
