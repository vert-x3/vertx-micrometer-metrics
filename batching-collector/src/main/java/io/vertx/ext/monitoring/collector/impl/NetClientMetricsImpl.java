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
import io.vertx.core.spi.metrics.TCPMetrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.*;

/**
 * @author Thomas Segismont
 */
public class NetClientMetricsImpl implements TCPMetrics<SocketAddress> {
  private final ConcurrentMap<SocketAddress, NetClientConnectionsMeasurements> connectionsMeasurements = new ConcurrentHashMap<>();
  private final NetClientMetricsSupplier netClientMetricsSupplier;

  public NetClientMetricsImpl(NetClientMetricsSupplier netClientMetricsSupplier) {
    this.netClientMetricsSupplier = netClientMetricsSupplier;
    netClientMetricsSupplier.register(this);
  }

  @Override
  public SocketAddress connected(SocketAddress remoteAddress, String remoteName) {
    SocketAddress key = new SocketAddressImpl(remoteAddress.port(), remoteName);
    connectionsMeasurements.computeIfAbsent(key, address -> new NetClientConnectionsMeasurements()).incrementConnections();
    return key;
  }

  @Override
  public void disconnected(SocketAddress key, SocketAddress remoteAddress) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.decrementConnections();
    }
  }

  @Override
  public void bytesRead(SocketAddress key, SocketAddress remoteAddress, long numberOfBytes) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.addBytesReceived(numberOfBytes);
    }
  }

  @Override
  public void bytesWritten(SocketAddress key, SocketAddress remoteAddress, long numberOfBytes) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.addBytesSent(numberOfBytes);
    }
  }

  @Override
  public void exceptionOccurred(SocketAddress key, SocketAddress remoteAddress, Throwable t) {
    NetClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.incrementErrorCount();
    }
  }

  /**
   * @return a snapshot of measurements for each remote address
   */
  public Map<SocketAddress, NetClientConnectionsMeasurements.Snapshot> getMeasurementsSnapshot() {
    return connectionsMeasurements.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().getSnapshot()));
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    netClientMetricsSupplier.unregister(this);
  }
}
