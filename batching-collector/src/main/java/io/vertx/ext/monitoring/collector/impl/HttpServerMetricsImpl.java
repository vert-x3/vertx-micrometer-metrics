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

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;

import java.util.concurrent.atomic.LongAdder;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author Thomas Segismont
 */
public class HttpServerMetricsImpl implements HttpServerMetrics<Long, Void, Void> {
  // Request info
  private final LongAdder processingTime = new LongAdder();
  private final LongAdder requestCount = new LongAdder();
  private final LongAdder requests = new LongAdder();
  // HTTP Connection info
  private final LongAdder httpConnections = new LongAdder();
  // Websocket Connection info
  private final LongAdder wsConnections = new LongAdder();
  // Bytes info
  private final LongAdder bytesReceived = new LongAdder();
  private final LongAdder bytesSent = new LongAdder();
  // Other
  private final LongAdder errorCount = new LongAdder();

  private final SocketAddress localAddress;
  private final HttpServerMetricsSupplier httpServerMetricsSupplier;

  public HttpServerMetricsImpl(SocketAddress localAddress, HttpServerMetricsSupplier httpServerMetricsSupplier) {
    this.localAddress = localAddress;
    this.httpServerMetricsSupplier = httpServerMetricsSupplier;
    httpServerMetricsSupplier.register(this);
  }

  @Override
  public Long requestBegin(Void socketMetric, HttpServerRequest request) {
    requests.increment();
    return System.nanoTime();
  }

  @Override
  public void requestReset(Long requestMetric) {
    requestCount.increment();
    requests.decrement();
  }

  @Override
  public Long responsePushed(Void socketMetric, HttpMethod method, String uri, HttpServerResponse response) {
    requests.increment();
    return System.nanoTime();
  }

  @Override
  public void responseEnd(Long nanoStart, HttpServerResponse response) {
    long requestProcessingTime = System.nanoTime() - nanoStart;
    processingTime.add(requestProcessingTime);
    requestCount.increment();
    requests.decrement();
  }

  @Override
  public Void upgrade(Long requestMetric, ServerWebSocket serverWebSocket) {
    return null;
  }

  @Override
  public Void connected(Void socketMetric, ServerWebSocket serverWebSocket) {
    wsConnections.increment();
    return null;
  }

  @Override
  public void disconnected(Void serverWebSocketMetric) {
    wsConnections.decrement();
  }

  @Override
  public Void connected(SocketAddress remoteAddress, String remoteName) {
    httpConnections.increment();
    return null;
  }

  @Override
  public void disconnected(Void socketMetric, SocketAddress remoteAddress) {
    httpConnections.decrement();
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    bytesReceived.add(numberOfBytes);
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    bytesSent.add(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    errorCount.increment();
  }

  /**
   * @return the local {@link SocketAddress} of the {@link io.vertx.core.http.HttpServer}
   */
  public SocketAddress getServerAddress() {
    return localAddress;
  }

  /**
   * @return cumulated processing time of http requests
   */
  public Long getProcessingTime() {
    return MILLISECONDS.convert(processingTime.sum(), NANOSECONDS);
  }

  /**
   * @return total number of processed http requests
   */
  public Long getRequestCount() {
    return requestCount.sum();
  }

  /**
   * @return number of http requests currently processed
   */
  public Long getRequests() {
    return requests.sum();
  }

  /**
   * @return number of http connections currently opened
   */
  public Long getHttpConnections() {
    return httpConnections.sum();
  }

  /**
   * @return number of websocket connections currently opened
   */
  public Long getWsConnections() {
    return wsConnections.sum();
  }

  /**
   * @return total number of bytes received
   */
  public Long getBytesReceived() {
    return bytesReceived.sum();
  }

  /**
   * @return total number of bytes sent
   */
  public Long getBytesSent() {
    return bytesSent.sum();
  }

  /**
   * @return total number of errors
   */
  public Long getErrorCount() {
    return errorCount.sum();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    httpServerMetricsSupplier.unregister(this);
  }
}
