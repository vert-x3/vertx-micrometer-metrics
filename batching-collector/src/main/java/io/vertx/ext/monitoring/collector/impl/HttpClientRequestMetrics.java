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

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.net.SocketAddress;

/**
 * HTTP request monitoring class. An instance is created whenever
 * {@link HttpClientMetricsImpl#requestBegin(Void, SocketAddress, SocketAddress, SocketAddress, HttpClientRequest)}
 * is called.
 * The instance is then associated with the request, until the response is received.
 *
 * @author Thomas Segismont
 */
public class HttpClientRequestMetrics {
  private final SocketAddress address;

  private long start;

  public HttpClientRequestMetrics(SocketAddress address) {
    this.address = address;
  }

  /**
   * @return remote address of the HTTP request
   */
  public SocketAddress getAddress() {
    return address;
  }

  /**
   * Sets the timer state to <em>now</em>.
   */
  public void resetTimer() {
    start = System.nanoTime();
  }

  /**
   * @return the number of nanoseconds elapsed since {@link #resetTimer()} was called
   */
  public long elapsed() {
    return System.nanoTime() - start;
  }
}
