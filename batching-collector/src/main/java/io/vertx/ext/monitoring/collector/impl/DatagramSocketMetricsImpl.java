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
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import static java.util.stream.Collectors.*;

/**
 * Implementation of {@link DatagramSocketMetrics} which relays data to {@link DatagramSocketMetricsSupplier}.
 *
 * @author Thomas Segismont
 */
public class DatagramSocketMetricsImpl implements DatagramSocketMetrics {
  private final ConcurrentMap<SocketAddress, LongAdder> bytesReceived = new ConcurrentHashMap<>();
  private final ConcurrentMap<SocketAddress, LongAdder> bytesSent = new ConcurrentHashMap<>();
  private final LongAdder errors = new LongAdder();
  private final DatagramSocketMetricsSupplier datagramSocketMetricsSupplier;

  private volatile SocketAddress localAddress;

  public DatagramSocketMetricsImpl(DatagramSocketMetricsSupplier datagramSocketMetricsSupplier) {
    this.datagramSocketMetricsSupplier = datagramSocketMetricsSupplier;
    datagramSocketMetricsSupplier.register(this);
  }

  @Override
  public void listening(String localName, SocketAddress localAddress) {
    this.localAddress = new SocketAddressImpl(localAddress.port(), localName);
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    LongAdder counter = bytesReceived.get(localAddress);
    if (counter == null) {
      counter = bytesReceived.computeIfAbsent(localAddress, address -> new LongAdder());
    }
    counter.add(numberOfBytes);
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    LongAdder counter = bytesSent.get(remoteAddress);
    if (counter == null) {
      counter = bytesSent.computeIfAbsent(remoteAddress, address -> new LongAdder());
    }
    counter.add(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    errors.increment();
  }

  /**
   * @return bytes received per remote {@link SocketAddress}
   */
  public Map<SocketAddress, Long> getBytesReceived() {
    return bytesReceived.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().sum()));
  }

  /**
   * @return bytes sent per remote {@link SocketAddress}
   */
  public Map<SocketAddress, Long> getBytesSent() {
    return bytesSent.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().sum()));
  }

  public long getErrorCount() {
    return errors.sum();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    datagramSocketMetricsSupplier.unregister(this);
  }
}
