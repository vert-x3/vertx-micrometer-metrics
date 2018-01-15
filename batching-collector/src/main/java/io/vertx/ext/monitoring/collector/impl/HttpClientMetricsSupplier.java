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
 * Aggregates values from {@link HttpClientMetricsImpl} instances and exposes metrics for collection.
 *
 * @author Thomas Segismont
 */
public class HttpClientMetricsSupplier implements MetricSupplier {
  private final String baseName;
  private final Set<HttpClientMetricsImpl> metricsSet = new CopyOnWriteArraySet<>();

  public HttpClientMetricsSupplier(String prefix) {
    baseName = prefix + (prefix.isEmpty() ? "" : ".") + "vertx.http.client.";
  }

  @Override
  public List<DataPoint> collect() {
    long timestamp = System.currentTimeMillis();

    Map<SocketAddress, HttpClientConnectionsMeasurements.Snapshot> values = new HashMap<>();

    for (HttpClientMetricsImpl httpClientMetrics : metricsSet) {
      httpClientMetrics.getMeasurementsSnapshot().forEach((address, snapshot) -> {
        values.merge(address, snapshot, HttpClientConnectionsMeasurements.Snapshot::merge);
      });
    }

    List<DataPoint> res = new ArrayList<>();

    values.forEach((address, snapshot) -> {
      String addressId = address.host() + ":" + address.port();
      // TCP metrics
      res.add(new GaugePoint(baseName + addressId + ".connections", timestamp, snapshot.getConnections()));
      res.add(new CounterPoint(baseName + addressId + ".bytesReceived", timestamp, snapshot.getBytesReceived()));
      res.add(new CounterPoint(baseName + addressId + ".bytesSent", timestamp, snapshot.getBytesSent()));
      res.add(new CounterPoint(baseName + addressId + ".errorCount", timestamp, snapshot.getErrorCount()));
      // HTTP metrics
      res.add(new GaugePoint(baseName + addressId + ".requests", timestamp, snapshot.getRequests()));
      res.add(new CounterPoint(baseName + addressId + ".requestCount", timestamp, snapshot.getRequestCount()));
      res.add(new CounterPoint(baseName + addressId + ".responseTime", timestamp, snapshot.getResponseTime()));
      res.add(new GaugePoint(baseName + addressId + ".wsConnections", timestamp, snapshot.getWsConnections()));
    });
    return res;
  }

  public void register(HttpClientMetricsImpl httpClientMetrics) {
    metricsSet.add(httpClientMetrics);
  }

  public void unregister(HttpClientMetricsImpl httpClientMetrics) {
    metricsSet.remove(httpClientMetrics);
  }
}
