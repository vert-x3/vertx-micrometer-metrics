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
package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.spi.metrics.EventBusMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Thomas Segismont
 */
public class EventBusMetricsImpl implements EventBusMetrics<EventBusHandlerMetrics>, MetricSupplier {
  private final String baseName;
  private final LongAdder handlers = new LongAdder();
  private final ConcurrentMap<String, HandlersMeasurements> handlersMeasurements = new ConcurrentHashMap<>();
  private final LongAdder errorCount = new LongAdder();
  private final LongAdder bytesWritten = new LongAdder();
  private final LongAdder bytesRead = new LongAdder();
  private final LongAdder pending = new LongAdder();
  private final LongAdder pendingLocal = new LongAdder();
  private final LongAdder pendingRemote = new LongAdder();
  private final LongAdder publishedMessages = new LongAdder();
  private final LongAdder publishedLocalMessages = new LongAdder();
  private final LongAdder publishedRemoteMessages = new LongAdder();
  private final LongAdder sentMessages = new LongAdder();
  private final LongAdder sentLocalMessages = new LongAdder();
  private final LongAdder sentRemoteMessages = new LongAdder();
  private final LongAdder receivedMessages = new LongAdder();
  private final LongAdder receivedLocalMessages = new LongAdder();
  private final LongAdder receivedRemoteMessages = new LongAdder();
  private final LongAdder deliveredMessages = new LongAdder();
  private final LongAdder deliveredLocalMessages = new LongAdder();
  private final LongAdder deliveredRemoteMessages = new LongAdder();
  private final LongAdder replyFailures = new LongAdder();

  public EventBusMetricsImpl(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.eventbus.";
  }

  @Override
  public EventBusHandlerMetrics handlerRegistered(String address, String repliedAddress) {
    handlers.increment();
    EventBusHandlerMetrics handlerMetrics = new EventBusHandlerMetrics(address);
    handlersMeasurements.compute(address, (key, value) -> value == null ? new HandlersMeasurements() : value.incrementHandlersCount());
    return handlerMetrics;
  }


  @Override
  public void handlerUnregistered(EventBusHandlerMetrics handlerMetrics) {
    handlers.decrement();
    String address = handlerMetrics.getAddress();
    handlersMeasurements.compute(address, (key, value) -> value.handlersCount() == 1 ? null : value.decrementHandlersCount());
  }

  @Override
  public void scheduleMessage(EventBusHandlerMetrics eventBusHandlerMetrics, boolean b) {
  }

  @Override
  public void beginHandleMessage(EventBusHandlerMetrics handlerMetrics, boolean local) {
    pending.decrement();
    if (local) {
      pendingLocal.decrement();
    } else {
      pendingRemote.decrement();
    }
    handlerMetrics.resetTimer();
  }

  @Override
  public void endHandleMessage(EventBusHandlerMetrics handlerMetrics, Throwable failure) {
    long elapsed = handlerMetrics.elapsed();
    HandlersMeasurements handlersMeasurements = this.handlersMeasurements.get(handlerMetrics.getAddress());
    if (handlersMeasurements != null) {
      handlersMeasurements.addProcessingTime(elapsed);
    }
    if (failure != null) {
      errorCount.increment();
    }
  }

  @Override
  public void messageSent(String address, boolean publish, boolean local, boolean remote) {
    if (publish) {
      publishedMessages.increment();
      if (local) {
        publishedLocalMessages.increment();
      } else {
        publishedRemoteMessages.increment();
      }
    } else {
      sentMessages.increment();
      if (local) {
        sentLocalMessages.increment();
      } else {
        sentRemoteMessages.increment();
      }
    }
  }

  @Override
  public void messageReceived(String address, boolean publish, boolean local, int handlers) {
    pending.add(handlers);
    receivedMessages.increment();
    if (local) {
      receivedLocalMessages.increment();
      pendingLocal.add(handlers);
    } else {
      receivedRemoteMessages.increment();
      pendingRemote.add(handlers);
    }
    if (handlers > 0) {
      deliveredMessages.increment();
      if (local) {
        deliveredLocalMessages.increment();
      } else {
        deliveredRemoteMessages.increment();
      }
    }
  }

  @Override
  public void messageWritten(String address, int numberOfBytes) {
    bytesWritten.add(numberOfBytes);
  }

  @Override
  public void messageRead(String address, int numberOfBytes) {
    bytesRead.add(numberOfBytes);
  }

  @Override
  public void replyFailure(String address, ReplyFailure failure) {
    replyFailures.increment();
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();
    List<DataPoint> dataPoints = new ArrayList<>();
    dataPoints.add(new GaugePoint(baseName + "handlers", timestamp, handlers.sum()));
    handlersMeasurements.entrySet().forEach(e -> {
      String address = e.getKey();
      HandlersMeasurements measurements = e.getValue();
      String source = address + ".processingTime";
      dataPoints.add(new CounterPoint(baseName + source, timestamp, measurements.processingTime()));
    });
    dataPoints.add(new CounterPoint(baseName + "errorCount", timestamp, errorCount.sum()));
    dataPoints.add(new CounterPoint(baseName + "bytesWritten", timestamp, bytesWritten.sum()));
    dataPoints.add(new CounterPoint(baseName + "bytesRead", timestamp, bytesRead.sum()));
    dataPoints.add(new GaugePoint(baseName + "pending", timestamp, pending.sum()));
    dataPoints.add(new GaugePoint(baseName + "pendingLocal", timestamp, pendingLocal.sum()));
    dataPoints.add(new GaugePoint(baseName + "pendingRemote", timestamp, pendingRemote.sum()));
    dataPoints.add(new CounterPoint(baseName + "publishedMessages", timestamp, publishedMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "publishedLocalMessages", timestamp, publishedLocalMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "publishedRemoteMessages", timestamp, publishedRemoteMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "sentMessages", timestamp, sentMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "sentLocalMessages", timestamp, sentLocalMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "sentRemoteMessages", timestamp, sentRemoteMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "receivedMessages", timestamp, receivedMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "receivedLocalMessages", timestamp, receivedLocalMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "receivedRemoteMessages", timestamp, receivedRemoteMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "deliveredMessages", timestamp, deliveredMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "deliveredLocalMessages", timestamp, deliveredLocalMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "deliveredRemoteMessages", timestamp, deliveredRemoteMessages.sum()));
    dataPoints.add(new CounterPoint(baseName + "replyFailures", timestamp, replyFailures.sum()));
    return dataPoints;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
  }

}
