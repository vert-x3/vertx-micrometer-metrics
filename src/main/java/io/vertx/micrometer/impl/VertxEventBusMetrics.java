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
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxEventBusMetrics extends AbstractMetrics implements EventBusMetrics<VertxEventBusMetrics.Handler> {
  private final static Handler IGNORED = new Handler(null);

  private final Gauges<LongAdder> handlers;
  private final Gauges<LongAdder> pending;
  private final Counters processed;
  private final Counters published;
  private final Counters sent;
  private final Counters received;
  private final Counters delivered;
  private final Counters discarded;
  private final Counters replyFailures;
  private final Summaries bytesRead;
  private final Summaries bytesWritten;

  VertxEventBusMetrics(MeterRegistry registry, boolean compatMode) {
    super(registry, MetricsDomain.EVENT_BUS);
    handlers = longGauges("handlers", "Number of event bus handlers in use", Label.EB_ADDRESS);
    pending = longGauges("pending", "Number of messages not processed yet", Label.EB_ADDRESS, Label.EB_SIDE);
    processed = counters("processed", "Number of processed messages", Label.EB_ADDRESS, Label.EB_SIDE);
    published = counters("published", "Number of messages published (publish / subscribe)", Label.EB_ADDRESS, Label.EB_SIDE);
    sent = counters("sent", "Number of messages sent (point-to-point)", Label.EB_ADDRESS, Label.EB_SIDE);
    received = counters("received", "Number of messages received", Label.EB_ADDRESS, Label.EB_SIDE);
    delivered = counters("delivered", "Number of messages delivered to handlers", Label.EB_ADDRESS, Label.EB_SIDE);
    discarded = counters("discarded", "Number of discarded messages", Label.EB_ADDRESS, Label.EB_SIDE);
    replyFailures = counters(compatMode ? "replyFailures" : "reply.failures", "Number of message reply failures", Label.EB_ADDRESS, Label.EB_FAILURE);
    bytesRead = summaries(compatMode ? "bytesRead" : "bytes.read", "Number of bytes received while reading messages from event bus cluster peers", Label.EB_ADDRESS);
    bytesWritten = summaries(compatMode ? "bytesWritten" : "bytes.written", "Number of bytes sent while sending messages to event bus cluster peers", Label.EB_ADDRESS);
  }

  private static boolean isInternal(String address) {
    return address.startsWith("__vertx.");
  }

  @Override
  public Handler handlerRegistered(String address, String repliedAddress) {
    if (isInternal(address)) {
      // Ignore internal metrics
      return IGNORED;
    }
    handlers.get(address).increment();
    return new Handler(address);
  }

  @Override
  public void handlerUnregistered(Handler handler) {
    if (isValid(handler)) {
      handlers.get(handler.address).decrement();
    }
  }

  @Override
  public void scheduleMessage(Handler handler, boolean b) {
  }

  @Override
  public void messageDelivered(Handler handler, boolean local) {
    if (isValid(handler)) {
      pending.get(handler.address, Labels.getSide(local)).decrement();
      processed.get(handler.address, Labels.getSide(local)).increment();
    }
  }

  @Override
  public void discardMessage(Handler handler, boolean local, Message<?> msg) {
    if (isValid(handler)) {
      pending.get(handler.address, Labels.getSide(local)).decrement();
      discarded.get(handler.address, Labels.getSide(local)).increment();
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (!isInternal(address)) {
      if (publish) {
        published.get(address, Labels.getSide(local)).increment();
      } else {
        sent.get(address, Labels.getSide(local)).increment();
      }
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    if (!isInternal(address)) {
      String origin = Labels.getSide(local);
      pending.get(address, origin).add(handlers);
      received.get(address, origin).increment();
      if (handlers > 0) {
        delivered.get(address, origin).increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    if (!isInternal(address)) {
      bytesWritten.get(address).record(numberOfBytes);
    }
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    if (!isInternal(address)) {
      bytesRead.get(address).record(numberOfBytes);
    }
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    if (!isInternal(address)) {
      replyFailures.get(address, failure.name()).increment();
    }
  }

  @Override
  public void close() {
  }

  private static boolean isValid(Handler handler) {
    return handler != null && handler.address != null;
  }

  static class Handler {
    private final String address;

    Handler(String address) {
      this.address = address;
    }
  }
}
