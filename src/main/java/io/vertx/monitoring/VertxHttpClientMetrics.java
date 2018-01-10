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

package io.vertx.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Counters;
import io.vertx.monitoring.meters.Gauges;
import io.vertx.monitoring.meters.Timers;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxHttpClientMetrics extends VertxNetClientMetrics {
  private final Gauges<LongAdder> requests;
  private final Counters requestCount;
  private final Timers responseTime;
  private final Counters responseCount;
  private final Gauges<LongAdder> wsConnections;

  VertxHttpClientMetrics(LabelMatchers labelMatchers, MeterRegistry registry) {
    super(labelMatchers, registry, MetricsCategory.HTTP_CLIENT, "vertx.http.client.");
    requests = longGauges("requests", "Number of requests waiting for a response", Labels.LOCAL, Labels.REMOTE, Labels.PATH);
    requestCount = counters("requestCount", "Number of requests sent", Labels.LOCAL, Labels.REMOTE, Labels.PATH, Labels.METHOD);
    responseTime = timers("responseTime", "Response time", Labels.LOCAL, Labels.REMOTE, Labels.PATH);
    responseCount = counters("responseCount", "Response count with codes", Labels.LOCAL, Labels.REMOTE, Labels.PATH, Labels.CODE);
    wsConnections = longGauges("wsConnections", "Number of websockets currently opened", Labels.LOCAL, Labels.REMOTE);
  }

  @Override
  HttpClientMetrics forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance extends VertxNetClientMetrics.Instance implements HttpClientMetrics<VertxHttpClientMetrics.Handler, String, String, Void, Void> {
    Instance(String localAddress) {
      super(localAddress);
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
    public void endpointConnected(Void endpointMetric, String socketMetric) {
    }

    @Override
    public void endpointDisconnected(Void endpointMetric, String socketMetric) {
    }

    @Override
    public Handler requestBegin(Void endpointMetric, String remote, SocketAddress localAddress, SocketAddress remoteAddress, HttpClientRequest request) {
      Handler handler = new Handler(remote, request.path());
      requests.get(labelMatchers, local, remote, handler.path).increment();
      requestCount.get(labelMatchers, local, remote, handler.path, request.method().name()).increment();
      handler.timer = responseTime.start(labelMatchers, local, remote, handler.path);
      return handler;
    }

    @Override
    public void requestEnd(Handler requestMetric) {
    }

    @Override
    public void responseBegin(Handler requestMetric, HttpClientResponse response) {
    }

    @Override
    public Handler responsePushed(Void endpointMetric, String remote, SocketAddress localAddress, SocketAddress remoteAddress, HttpClientRequest request) {
      return requestBegin(null, remote, localAddress, remoteAddress, request);
    }

    @Override
    public void requestReset(Handler handler) {
      requests.get(labelMatchers, local, handler.address, handler.path).decrement();
    }

    @Override
    public void responseEnd(Handler handler, HttpClientResponse response) {
      requests.get(labelMatchers, local, handler.address, handler.path).decrement();
      responseCount.get(labelMatchers, local, handler.address, handler.path, String.valueOf(response.statusCode()));
      handler.timer.end();
    }

    @Override
    public String connected(Void endpointMetric, String remote, WebSocket webSocket) {
      wsConnections.get(labelMatchers, local, remote).increment();
      return remote;
    }

    @Override
    public void disconnected(String remote) {
      wsConnections.get(labelMatchers, local, remote).decrement();
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public void close() {
    }
  }

  public static class Handler {
    private final String address;
    private final String path;
    private Timers.EventTiming timer;

    Handler(String address, String path) {
      this.address = address;
      this.path = path;
    }
  }
}
