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
 * Aggregates values from {@link NetServerMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class NetServerMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<NetServerMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public NetServerMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.net.server.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();

    Map<SocketAddress, Long> connections = new HashMap<>();
    Map<SocketAddress, Long> bytesReceived = new HashMap<>();
    Map<SocketAddress, Long> bytesSent = new HashMap<>();
    Map<SocketAddress, Long> errorCount = new HashMap<>();

    for (NetServerMetricsImpl netServerMetrics : metricsSet) {
      SocketAddress serverAddress = netServerMetrics.getServerAddress();
      merge(connections, serverAddress, netServerMetrics.getConnections());
      merge(bytesReceived, serverAddress, netServerMetrics.getBytesReceived());
      merge(bytesSent, serverAddress, netServerMetrics.getBytesSent());
      merge(errorCount, serverAddress, netServerMetrics.getErrorCount());
    }

    List<DataPoint> res = new ArrayList<>();
    res.addAll(gauges("connections", timestamp, connections));
    res.addAll(counters("bytesReceived", timestamp, bytesReceived));
    res.addAll(counters("bytesSent", timestamp, bytesSent));
    res.addAll(counters("errorCount", timestamp, errorCount));

    return res;
  }

  private void merge(Map<SocketAddress, Long> values, SocketAddress serverAddress, Long value) {
    values.merge(serverAddress, value, Long::sum);
  }

  private List<DataPoint> gauges(String id, long timestamp, Map<SocketAddress, Long> values) {
    List<DataPoint> res = new ArrayList<>(values.size());
    values.forEach((address, count) -> {
      String name = baseName + address.host() + ":" + address.port() + "." + id;
      res.add(new GaugePoint(name, timestamp, count));
    });
    return res;
  }

  private List<DataPoint> counters(String id, long timestamp, Map<SocketAddress, Long> values) {
    List<DataPoint> res = new ArrayList<>(values.size());
    values.forEach((address, count) -> {
      String name = baseName + address.host() + ":" + address.port() + "." + id;
      res.add(new CounterPoint(name, timestamp, count));
    });
    return res;
  }

  public void register(NetServerMetricsImpl netServerMetrics) {
    metricsSet.add(netServerMetrics);
  }

  public void unregister(NetServerMetricsImpl netServerMetrics) {
    metricsSet.remove(netServerMetrics);
  }
}
