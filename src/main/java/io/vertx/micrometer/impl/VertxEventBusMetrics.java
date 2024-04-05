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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.impl.VertxEventBusMetrics.HandlerMetric;
import io.vertx.micrometer.impl.tags.Labels;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.EVENT_BUS;

/**
 * @author Joel Takvorian
 */
class VertxEventBusMetrics extends AbstractMetrics implements EventBusMetrics<HandlerMetric> {

  private final MeterProvider<Counter> ebPublished;
  private final MeterProvider<Counter> ebSent;
  private final MeterProvider<Counter> ebReceived;
  private final MeterProvider<Counter> ebDelivered;
  private final MeterProvider<DistributionSummary> ebBytesWritten;
  private final MeterProvider<DistributionSummary> ebBytesRead;
  private final MeterProvider<Counter> ebReplyFailures;

  VertxEventBusMetrics(AbstractMetrics parent) {
    super(parent, EVENT_BUS);
    ebPublished = Counter.builder(names.getEbPublished())
      .description("Number of messages published (publish / subscribe)")
      .withRegistry(registry);
    ebSent = Counter.builder(names.getEbSent())
      .description("Number of messages sent (point-to-point)")
      .withRegistry(registry);
    ebReceived = Counter.builder(names.getEbReceived())
      .description("Number of messages received")
      .withRegistry(registry);
    ebDelivered = Counter.builder(names.getEbDelivered())
      .description("Number of messages delivered to handlers")
      .withRegistry(registry);
    ebBytesWritten = DistributionSummary.builder(names.getEbBytesWritten())
      .description("Number of bytes sent while sending messages to event bus cluster peers")
      .withRegistry(registry);
    ebBytesRead = DistributionSummary.builder(names.getEbBytesRead())
      .description("Number of bytes received while reading messages from event bus cluster peers")
      .withRegistry(registry);
    ebReplyFailures = Counter.builder(names.getEbReplyFailures())
      .description("Number of message reply failures")
      .withRegistry(registry);
  }

  private static boolean isNotInternal(String address) {
    return !address.startsWith("__vertx.");
  }

  @Override
  public HandlerMetric handlerRegistered(String address) {
    if (isNotInternal(address)) {
      HandlerMetric handlerMetric = new HandlerMetric(address);
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
      Tags tags = addressAndSide(address, local);
      if (publish) {
        ebPublished.withTags(tags).increment();
      } else {
        ebSent.withTags(tags).increment();
      }
    }
  }

  private Tags addressAndSide(String address, boolean local) {
    Tags tags = Tags.empty();
    if (enabledLabels.contains(EB_ADDRESS)) {
      tags = tags.and(EB_ADDRESS.toString(), address);
    }
    if (enabledLabels.contains(EB_SIDE)) {
      tags = tags.and(Labels.side(local));
    }
    return tags;
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    if (isNotInternal(address)) {
      Tags tags = addressAndSide(address, local);
      ebReceived.withTags(tags).increment();
      if (handlers > 0) {
        longGaugeBuilder(names.getEbPending(), LongAdder::doubleValue)
          .description("Number of messages not processed yet")
          .tags(tags)
          .register(registry)
          .add(handlers);
        ebDelivered.withTags(tags).increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      Tags tags = address(address);
      ebBytesWritten.withTags(tags).record(numberOfBytes);
    }
  }

  private Tags address(String address) {
    Tags tags = Tags.empty();
    if (enabledLabels.contains(EB_ADDRESS)) {
      tags = tags.and(EB_ADDRESS.toString(), address);
    }
    return tags;
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    if (isNotInternal(address)) {
      Tags tags = address(address);
      ebBytesRead.withTags(tags).record(numberOfBytes);
    }
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    if (isNotInternal(address)) {
      Tags tags = addressAndFailure(address, failure);
      ebReplyFailures.withTags(tags).increment();
    }
  }

  private Tags addressAndFailure(String address, ReplyFailure replyFailure) {
    Tags tags = Tags.empty();
    if (enabledLabels.contains(EB_ADDRESS)) {
      tags = tags.and(EB_ADDRESS.toString(), address);
    }
    if (enabledLabels.contains(EB_FAILURE)) {
      tags = tags.and(EB_FAILURE.toString(), replyFailure.name());
    }
    return tags;
  }

  class HandlerMetric {

    final LongAdder handlers;
    final LongAdder ebPendingLocal;
    final Counter ebProcessedLocal;
    final LongAdder ebPendingRemote;
    final Counter ebProcessedRemote;
    final Counter ebDiscardedLocal;
    final Counter ebDiscardedRemote;

    HandlerMetric(String address) {
      Tags tags = Tags.empty();
      if (enabledLabels.contains(EB_ADDRESS)) {
        tags = tags.and(EB_ADDRESS.toString(), address);
      }
      handlers = longGaugeBuilder(names.getEbHandlers(), LongAdder::doubleValue)
        .description("Number of event bus handlers in use")
        .tags(tags)
        .register(registry);
      Tags localTags = tags, remoteTags = tags;
      if (enabledLabels.contains(EB_SIDE)) {
        localTags = tags.and(Labels.side(true));
        remoteTags = tags.and(Labels.side(false));
      }
      ebPendingLocal = longGaugeBuilder(names.getEbPending(), LongAdder::doubleValue)
        .description("Number of messages not processed yet")
        .tags(localTags)
        .register(registry);
      ebProcessedLocal = Counter.builder(names.getEbProcessed())
        .description("Number of processed messages")
        .tags(localTags)
        .register(registry);
      ebDiscardedLocal = Counter.builder(names.getEbDiscarded())
        .description("Number of discarded messages")
        .tags(localTags)
        .register(registry);
      ebPendingRemote = longGaugeBuilder(names.getEbPending(), LongAdder::doubleValue)
        .description("Number of messages not processed yet")
        .tags(remoteTags)
        .register(registry);
      ebProcessedRemote = Counter.builder(names.getEbProcessed())
        .description("Number of processed messages")
        .tags(remoteTags)
        .register(registry);
      ebDiscardedRemote = Counter.builder(names.getEbDiscarded())
        .description("Number of discarded messages")
        .tags(remoteTags)
        .register(registry);
    }
  }
}
