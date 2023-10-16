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
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.impl.VertxEventBusMetrics.HandlerMetric;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.EVENT_BUS;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;
import static java.util.function.UnaryOperator.identity;

/**
 * @author Joel Takvorian
 */
class VertxEventBusMetrics extends AbstractMetrics implements EventBusMetrics<HandlerMetric> {

  VertxEventBusMetrics(AbstractMetrics parent) {
    super(parent, EVENT_BUS);
  }

  private static boolean isNotInternal(String address) {
    return !address.startsWith("__vertx.");
  }

  @Override
  public HandlerMetric handlerRegistered(String address) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      HandlerMetric handlerMetric = new HandlerMetric(tags);
      handlerMetric.handlers.increment();
      return handlerMetric;
    }
    return null;
  }

  @Override
  public void handlerUnregistered(HandlerMetric handlerMetric) {
    if (handlerMetric != null) {
      handlerMetric.handlers.decrement();
    }
  }

  @Override
  public void messageDelivered(HandlerMetric handlerMetric, boolean local) {
    if (handlerMetric != null) {
      if (local) {
        handlerMetric.ebPendingLocal.decrement();
        handlerMetric.ebProcessedLocal.increment();
      } else {
        handlerMetric.ebPendingRemote.decrement();
        handlerMetric.ebProcessedRemote.increment();
      }
    }
  }

  @Override
  public void discardMessage(HandlerMetric handlerMetric, boolean local, Message<?> msg) {
    if (handlerMetric != null) {
      if (local) {
        handlerMetric.ebPendingLocal.decrement();
        handlerMetric.ebDiscardedLocal.increment();
      } else {
        handlerMetric.ebPendingRemote.decrement();
        handlerMetric.ebDiscardedRemote.increment();
      }
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_SIDE, Labels::side, local));
      if (publish) {
        counter(names.getEbPublished(), "Number of messages published (publish / subscribe)", tags.unwrap())
          .increment();
      } else {
        counter(names.getEbSent(), "Number of messages sent (point-to-point)", tags.unwrap())
          .increment();
      }
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_SIDE, Labels::side, local));
      counter(names.getEbReceived(), "Number of messages received", tags.unwrap())
        .increment();
      if (handlers > 0) {
        longGauge(names.getEbPending(), "Number of messages not processed yet", tags.unwrap())
          .add(handlers);
        counter(names.getEbDelivered(), "Number of messages delivered to handlers", tags.unwrap())
          .increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      distributionSummary(names.getEbBytesWritten(), "Number of bytes sent while sending messages to event bus cluster peers", tags.unwrap())
        .record(numberOfBytes);
    }
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address));
      distributionSummary(names.getEbBytesRead(), "Number of bytes received while reading messages from event bus cluster peers", tags.unwrap())
        .record(numberOfBytes);
    }
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    if (isNotInternal(address)) {
      TagsWrapper tags = of(toTag(EB_ADDRESS, identity(), address), toTag(EB_FAILURE, ReplyFailure::name, failure));
      counter(names.getEbReplyFailures(), "Number of message reply failures", tags.unwrap()).increment();
    }
  }

  class HandlerMetric {

    final LongAdder handlers;
    final LongAdder ebPendingLocal;
    final Counter ebProcessedLocal;
    final LongAdder ebPendingRemote;
    final Counter ebProcessedRemote;
    final Counter ebDiscardedLocal;
    final Counter ebDiscardedRemote;

    HandlerMetric(TagsWrapper tags) {
      handlers = longGauge(names.getEbHandlers(), "Number of event bus handlers in use", tags.unwrap());
      TagsWrapper localTags = tags.and(toTag(EB_SIDE, Labels::side, true));
      ebPendingLocal = longGauge(names.getEbPending(), "Number of messages not processed yet", localTags.unwrap());
      ebProcessedLocal = counter(names.getEbProcessed(), "Number of processed messages", localTags.unwrap());
      ebDiscardedLocal = counter(names.getEbDiscarded(), "Number of discarded messages", localTags.unwrap());
      TagsWrapper remoteTags = tags.and(toTag(EB_SIDE, Labels::side, false));
      ebPendingRemote = longGauge(names.getEbPending(), "Number of messages not processed yet", remoteTags.unwrap());
      ebProcessedRemote = counter(names.getEbProcessed(), "Number of processed messages", remoteTags.unwrap());
      ebDiscardedRemote = counter(names.getEbDiscarded(), "Number of discarded messages", remoteTags.unwrap());
    }
  }
}
