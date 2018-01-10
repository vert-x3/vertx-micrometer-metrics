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
 * Aggregates values from {@link HttpServerMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class HttpServerMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<HttpServerMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public HttpServerMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.http.server.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();

    Map<SocketAddress, Long> processingTime = new HashMap<>();
    Map<SocketAddress, Long> requestCount = new HashMap<>();
    Map<SocketAddress, Long> requests = new HashMap<>();
    Map<SocketAddress, Long> httpConnections = new HashMap<>();
    Map<SocketAddress, Long> wsConnections = new HashMap<>();
    Map<SocketAddress, Long> bytesReceived = new HashMap<>();
    Map<SocketAddress, Long> bytesSent = new HashMap<>();
    Map<SocketAddress, Long> errorCount = new HashMap<>();

    for (HttpServerMetricsImpl httpServerMetrics : metricsSet) {
      SocketAddress serverAddress = httpServerMetrics.getServerAddress();
      merge(processingTime, serverAddress, httpServerMetrics.getProcessingTime());
      merge(requestCount, serverAddress, httpServerMetrics.getRequestCount());
      merge(requests, serverAddress, httpServerMetrics.getRequests());
      merge(httpConnections, serverAddress, httpServerMetrics.getHttpConnections());
      merge(wsConnections, serverAddress, httpServerMetrics.getWsConnections());
      merge(bytesReceived, serverAddress, httpServerMetrics.getBytesReceived());
      merge(bytesSent, serverAddress, httpServerMetrics.getBytesSent());
      merge(errorCount, serverAddress, httpServerMetrics.getErrorCount());
    }

    List<DataPoint> res = new ArrayList<>();
    res.addAll(counters("processingTime", timestamp, processingTime));
    res.addAll(counters("requestCount", timestamp, requestCount));
    res.addAll(gauges("requests", timestamp, requests));
    res.addAll(gauges("httpConnections", timestamp, httpConnections));
    res.addAll(gauges("wsConnections", timestamp, wsConnections));
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

  public void register(HttpServerMetricsImpl httpServerMetrics) {
    metricsSet.add(httpServerMetrics);
  }

  public void unregister(HttpServerMetricsImpl httpServerMetrics) {
    metricsSet.remove(httpServerMetrics);
  }
}
