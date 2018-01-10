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

import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Thomas Segismont
 */
public class NetServerMetricsImpl implements TCPMetrics<Void> {
  // Connection info
  private final LongAdder connections = new LongAdder();
  // Bytes info
  private final LongAdder bytesReceived = new LongAdder();
  private final LongAdder bytesSent = new LongAdder();
  // Other
  private final LongAdder errorCount = new LongAdder();

  private final SocketAddress localAddress;
  private final NetServerMetricsSupplier netServerMetricsSupplier;

  public NetServerMetricsImpl(SocketAddress localAddress, NetServerMetricsSupplier netServerMetricsSupplier) {
    this.localAddress = localAddress;
    this.netServerMetricsSupplier = netServerMetricsSupplier;
    netServerMetricsSupplier.register(this);
  }

  @Override
  public Void connected(SocketAddress remoteAddress, String remoteName) {
    connections.increment();
    return null;
  }

  @Override
  public void disconnected(Void socketMetric, SocketAddress remoteAddress) {
    connections.decrement();
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
   * @return the local {@link SocketAddress} of the {@link io.vertx.core.net.NetServer}
   */
  public SocketAddress getServerAddress() {
    return localAddress;
  }

  /**
   * @return number of connections currently opened
   */
  public long getConnections() {
    return connections.sum();
  }

  /**
   * @return total number of bytes received
   */
  public long getBytesReceived() {
    return bytesReceived.sum();
  }

  /**
   * @return total number of bytes sent
   */
  public long getBytesSent() {
    return bytesSent.sum();
  }

  /**
   * @return total number of errors
   */
  public long getErrorCount() {
    return errorCount.sum();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    netServerMetricsSupplier.unregister(this);
  }
}
