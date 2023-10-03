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
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;
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
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      Handler handler = new Handler(tags);
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
      TagsWrapper tags = handler.tags.and(toTag(EB_SIDE, Labels::side, local));
      longGauge(names.getEbPending())
        .description("Number of messages not processed yet")
        .tags(tags.unwrap())
        .register(registry)
        .decrement();
      counter(names.getEbProcessed())
        .description("Number of processed messages")
        .tags(tags.unwrap())
        .register(registry)
        .increment();
    }
  }

  @Override
  public void discardMessage(Handler handler, boolean local, Message<?> msg) {
    if (handler != null) {
      TagsWrapper tags = handler.tags.and(toTag(EB_SIDE, Labels::side, local));
      longGauge(names.getEbPending())
        .description("Number of messages not processed yet")
        .tags(tags.unwrap())
        .register(registry)
        .decrement();
      counter(names.getEbDiscarded())
        .description("Number of discarded messages")
        .tags(tags.unwrap())
        .register(registry)
        .increment();
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_SIDE, Labels::side, local));
      if (publish) {
        counter(names.getEbPublished())
          .description("Number of messages published (publish / subscribe)")
          .tags(tags.unwrap())
          .register(registry)
          .increment();
      } else {
        counter(names.getEbSent())
          .description("Number of messages sent (point-to-point)")
          .tags(tags.unwrap())
          .register(registry)
          .increment();
      }
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_SIDE, Labels::side, local));
      counter(names.getEbReceived())
        .description("Number of messages received")
        .tags(tags.unwrap())
        .register(registry)
        .increment();
      if (handlers > 0) {
        longGauge(names.getEbPending())
          .description("Number of messages not processed yet")
          .tags(tags.unwrap())
          .register(registry)
          .add(handlers);
        counter(names.getEbDelivered())
          .description("Number of messages delivered to handlers")
          .tags(tags.unwrap())
          .register(registry)
          .increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      distributionSummary(names.getEbBytesWritten())
        .description("Number of bytes sent while sending messages to event bus cluster peers")
        .tags(tags.unwrap())
        .register(registry)
        .record(numberOfBytes);
    }
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      distributionSummary(names.getEbBytesRead())
        .description("Number of bytes received while reading messages from event bus cluster peers")
        .tags(tags.unwrap())
        .register(registry)
        .record(numberOfBytes);
    }
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_FAILURE, ReplyFailure::name, failure));
      counter(names.getEbReplyFailures())
        .description("Number of message reply failures")
        .tags(tags.unwrap())
        .register(registry).increment();
    }
  }

  class Handler {

    final TagsWrapper tags;
    final LongAdder handlers;

    Handler(TagsWrapper tags) {
      this.tags = tags;
      handlers = longGauge(names.getEbHandlers())
        .description("Number of event bus handlers in use")
        .tags(tags.unwrap())
        .register(registry);
    }
  }
}
