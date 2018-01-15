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
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.metrics.HttpClientMetrics;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.*;

/**
 * @author Thomas Segismont
 */
public class HttpClientMetricsImpl
  implements HttpClientMetrics<HttpClientRequestMetrics, SocketAddress, SocketAddress, Void, Void> {

  private final ConcurrentMap<SocketAddress, HttpClientConnectionsMeasurements> connectionsMeasurements = new ConcurrentHashMap<>();
  private final HttpClientMetricsSupplier httpClientMetricsSupplier;

  public HttpClientMetricsImpl(HttpClientMetricsSupplier httpClientMetricsSupplier) {
    this.httpClientMetricsSupplier = httpClientMetricsSupplier;
    httpClientMetricsSupplier.register(this);
  }

  @Override
  public Void createEndpoint(String host, int port, int maxPoolSize) {
    return null;
  }

  @Override
  public void closeEndpoint(String host, int port, Void endpointMetric) {

  }

  @Override
  public Void enqueueRequest(Void endpointMetric) {
    return null;
  }

  @Override
  public void dequeueRequest(Void endpointMetric, Void taskMetric) {

  }

  @Override
  public void endpointConnected(Void endpointMetric, SocketAddress socketMetric) {

  }

  @Override
  public void endpointDisconnected(Void endpointMetric, SocketAddress socketMetric) {

  }

  @Override
  public void requestEnd(HttpClientRequestMetrics requestMetric) {

  }

  @Override
  public void responseBegin(HttpClientRequestMetrics requestMetric, HttpClientResponse response) {

  }

  @Override
  public HttpClientRequestMetrics requestBegin(Void endpointMetric, SocketAddress key, SocketAddress localAddress, SocketAddress remoteAddress, HttpClientRequest request) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.requestBegin();
    }
    HttpClientRequestMetrics httpClientRequestMetrics = new HttpClientRequestMetrics(key);
    httpClientRequestMetrics.resetTimer();
    return httpClientRequestMetrics;
  }

  @Override
  public HttpClientRequestMetrics responsePushed(Void endpointMetric, SocketAddress key, SocketAddress localAddress, SocketAddress remoteAddress, HttpClientRequest request) {
    return requestBegin(null, key, localAddress, remoteAddress, request);
  }

  @Override
  public void requestReset(HttpClientRequestMetrics requestMetric) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(requestMetric.getAddress());
    if (measurements != null) {
      measurements.requestReset();
    }
  }

  @Override
  public void responseEnd(HttpClientRequestMetrics requestMetric, HttpClientResponse response) {
    long responseTime = requestMetric.elapsed();
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(requestMetric.getAddress());
    if (measurements != null) {
      measurements.responseEnd(responseTime);
    }
  }

  @Override
  public SocketAddress connected(Void endpointMetric, SocketAddress key, WebSocket webSocket) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.incrementWsConnectionCount();
    }
    return key;
  }

  @Override
  public void disconnected(SocketAddress key) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.decrementWsConnectionCount();
    }
  }

  @Override
  public SocketAddress connected(SocketAddress remoteAddress, String remoteName) {
    SocketAddress key = new SocketAddressImpl(remoteAddress.port(), remoteName);
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements == null) {
      measurements = connectionsMeasurements.computeIfAbsent(key, address -> new HttpClientConnectionsMeasurements());
    }
    measurements.incrementConnections();
    return key;
  }

  @Override
  public void disconnected(SocketAddress key, SocketAddress remoteAddress) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.decrementConnections();
    }
  }

  @Override
  public void bytesRead(SocketAddress key, SocketAddress remoteAddress, long numberOfBytes) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.addBytesReceived(numberOfBytes);
    }
  }

  @Override
  public void bytesWritten(SocketAddress key, SocketAddress remoteAddress, long numberOfBytes) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.addBytesSent(numberOfBytes);
    }
  }

  @Override
  public void exceptionOccurred(SocketAddress key, SocketAddress remoteAddress, Throwable t) {
    HttpClientConnectionsMeasurements measurements = connectionsMeasurements.get(key);
    if (measurements != null) {
      measurements.incrementErrorCount();
    }
  }

  /**
   * @return a snapshot of measurements for each remote address
   */
  public Map<SocketAddress, HttpClientConnectionsMeasurements.Snapshot> getMeasurementsSnapshot() {
    return connectionsMeasurements.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().getSnapshot()));
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    httpClientMetricsSupplier.unregister(this);
  }
}
