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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Aggregates values from {@link DatagramSocketMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class DatagramSocketMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<DatagramSocketMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public DatagramSocketMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.datagram.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();
    Map<SocketAddress, Long> received = new HashMap<>();
    Map<SocketAddress, Long> sent = new HashMap<>();
    long errorCount = 0;
    for (DatagramSocketMetricsImpl datagramSocketMetrics : metricsSet) {
      datagramSocketMetrics.getBytesReceived().forEach((address, bytes) -> received.merge(address, bytes, Long::sum));
      datagramSocketMetrics.getBytesSent().forEach((address, bytes) -> sent.merge(address, bytes, Long::sum));
      errorCount += datagramSocketMetrics.getErrorCount();
    }
    List<DataPoint> res = new ArrayList<>(received.size() + sent.size() + 1);
    received.forEach((address, count) -> {
      String addressId = address.host() + ":" + address.port();
      res.add(new CounterPoint(baseName + addressId + ".bytesReceived", timestamp, count));
    });
    sent.forEach((address, count) -> {
      String addressId = address.host() + ":" + address.port();
      res.add(new CounterPoint(baseName + addressId + ".bytesSent", timestamp, count));
    });
    res.add(new CounterPoint(baseName + "errorCount", timestamp, errorCount));
    return res;
  }

  public void register(DatagramSocketMetricsImpl datagramSocketMetrics) {
    metricsSet.add(datagramSocketMetrics);
  }

  public void unregister(DatagramSocketMetricsImpl datagramSocketMetrics) {
    metricsSet.remove(datagramSocketMetrics);
  }
}
