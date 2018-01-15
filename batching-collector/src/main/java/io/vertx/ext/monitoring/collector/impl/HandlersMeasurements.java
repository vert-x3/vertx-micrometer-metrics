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

import java.util.concurrent.atomic.LongAdder;

import static java.util.concurrent.TimeUnit.*;

/**
 * Holds measurements for all handlers of an event bus address. An instance is created when the first handler is
 * registered, then handlers are counted with {@link #incrementHandlersCount()} and {@link #decrementHandlersCount()}.
 *
 * @author Thomas Segismont
 */
public class HandlersMeasurements {
  private final LongAdder processingTime;
  private final int handlersCount;

  /**
   * Creates a new instance with a handlers reference of 1.
   */
  public HandlersMeasurements() {
    processingTime = new LongAdder();
    handlersCount = 1;
  }

  private HandlersMeasurements(LongAdder processingTime, int handlersCount) {
    this.processingTime = processingTime;
    this.handlersCount = handlersCount;
  }

  /**
   * Increments total processing time.
   *
   * @param time processing time to add, in nanoseconds
   */
  public void addProcessingTime(long time) {
    processingTime.add(time);
  }

  /**
   * @return the total processing time, in milliseconds
   */
  public long processingTime() {
    return MILLISECONDS.convert(processingTime.sum(), NANOSECONDS);
  }

  /**
   * @return number of handlers of a same address
   */
  public int handlersCount() {
    return handlersCount;
  }

  /**
   * @return a new instance of this class, with same processing time and a handler count incremented by 1
   */
  public HandlersMeasurements incrementHandlersCount() {
    return new HandlersMeasurements(processingTime, handlersCount + 1);
  }

  /**
   * @return a new instance of this class, with same processing time and a handler count decremented by 1
   */
  public HandlersMeasurements decrementHandlersCount() {
    return new HandlersMeasurements(processingTime, handlersCount - 1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HandlersMeasurements that = (HandlersMeasurements) o;
    return handlersCount == that.handlersCount;
  }

  @Override
  public int hashCode() {
    return handlersCount;
  }
}
