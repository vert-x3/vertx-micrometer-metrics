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
package io.vertx.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.micrometer.meters.Counters;
import io.vertx.micrometer.meters.Gauges;
import io.vertx.micrometer.meters.Summaries;
import io.vertx.micrometer.meters.Timers;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxEventBusMetrics extends AbstractMetrics implements EventBusMetrics<VertxEventBusMetrics.Handler> {
  private final Gauges<LongAdder> handlers;
  private final Gauges<LongAdder> pending;
  private final Counters published;
  private final Counters sent;
  private final Counters received;
  private final Counters delivered;
  private final Counters errorCount;
  private final Counters replyFailures;
  private final Timers processTime;
  private final Summaries bytesRead;
  private final Summaries bytesWritten;

  VertxEventBusMetrics(MeterRegistry registry) {
    super(registry, MetricsDomain.EVENT_BUS);
    handlers = longGauges("handlers", "Number of event bus handlers in use", Label.ADDRESS);
    pending = longGauges("pending", "Number of messages not processed yet", Label.ADDRESS, Label.SIDE);
    published = counters("published", "Number of messages published (publish / subscribe)", Label.ADDRESS, Label.SIDE);
    sent = counters("sent", "Number of messages sent (point-to-point)", Label.ADDRESS, Label.SIDE);
    received = counters("received", "Number of messages received", Label.ADDRESS, Label.SIDE);
    delivered = counters("delivered", "Number of messages delivered to handlers", Label.ADDRESS, Label.SIDE);
    errorCount = counters("errors", "Number of errors", Label.ADDRESS, Label.CLASS);
    replyFailures = counters("replyFailures", "Number of message reply failures", Label.ADDRESS, Label.FAILURE);
    processTime = timers("processingTime", "Processing time", Label.ADDRESS);
    bytesRead = summaries("bytesRead", "Number of bytes received while reading messages from event bus cluster peers", Label.ADDRESS);
    bytesWritten = summaries("bytesWritten", "Number of bytes sent while sending messages to event bus cluster peers", Label.ADDRESS);
  }

  @Override
  public Handler handlerRegistered(String address, String repliedAddress) {
    handlers.get(address).increment();
    return new Handler(address);
  }

  @Override
  public void handlerUnregistered(Handler handler) {
    handlers.get(handler.address).decrement();
  }

  @Override
  public void scheduleMessage(Handler handler, boolean b) {
  }

  @Override
  public void beginHandleMessage(Handler handler, boolean local) {
    pending.get(handler.address, Labels.getSide(local)).decrement();
    handler.timer = processTime.start(handler.address);
  }

  @Override
  public void endHandleMessage(Handler handler, Throwable failure) {
    handler.timer.end();
    if (failure != null) {
      errorCount.get(handler.address, failure.getClass().getSimpleName()).increment();
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (publish) {
      published.get( address, Labels.getSide(local)).increment();
    } else {
      sent.get(address, Labels.getSide(local)).increment();
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    String origin = Labels.getSide(local);
    pending.get(address, origin).add(handlers);
    received.get(address, origin).increment();
    if (handlers > 0) {
      delivered.get(address, origin).increment();
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    bytesWritten.get(address).record(numberOfBytes);
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    bytesRead.get(address).record(numberOfBytes);
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    replyFailures.get(address, failure.name()).increment();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
  }

  public static class Handler {
    private final String address;
    private Timers.EventTiming timer;

    Handler(String address) {
      this.address = address;
    }
  }
}
