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

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Timers;

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

  VertxHttpClientMetrics(MeterRegistry registry) {
    super(registry, MetricsDomain.HTTP_CLIENT);
    requests = longGauges("requests", "Number of requests waiting for a response", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestCount = counters("requestCount", "Number of requests sent", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    responseTime = timers("responseTime", "Response time", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    responseCount = counters("responseCount", "Response count with codes", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    wsConnections = longGauges("wsConnections", "Number of websockets currently opened", Label.LOCAL, Label.REMOTE);
  }

  @Override
  HttpClientMetrics forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance extends VertxNetClientMetrics.Instance implements HttpClientMetrics<VertxHttpClientMetrics.Handler, String, String, Void> {
    Instance(String localAddress) {
      super(localAddress);
    }

    @Override
    public ClientMetrics<Handler, Void, HttpClientRequest, HttpClientResponse> createEndpointMetrics(SocketAddress remoteAddress, int maxPoolSize) {
      String remote = remoteAddress.toString();
      return new ClientMetrics<Handler, Void, HttpClientRequest, HttpClientResponse>() {
        @Override
        public Handler requestBegin(String uri, HttpClientRequest request) {
          Handler handler = new Handler(remote, request.path(),request.method().name());
          requests.get(local, remote, handler.path, handler.method).increment();
          requestCount.get(local, remote, handler.path, handler.method).increment();
          handler.timer = responseTime.start();
          return handler;
        }
        @Override
        public void requestReset(Handler handler) {
          requests.get(local, handler.address, handler.path, handler.method).decrement();
        }
        @Override
        public void responseEnd(Handler handler, HttpClientResponse response) {
          String code = String.valueOf(response.statusCode());
          requests.get(local, handler.address, handler.path, handler.method).decrement();
          responseCount.get(local, handler.address, handler.path, handler.method, code).increment();
          handler.timer.end(local, handler.address, handler.path, handler.method, code);
        }
      };
    }


    @Override
    public String connected(WebSocket webSocket) {
      String remote = webSocket.remoteAddress().toString();
      wsConnections.get(local, remote).increment();
      return remote;
    }

    @Override
    public void disconnected(String remote) {
      wsConnections.get(local, remote).decrement();
    }

    @Override
    public void close() {
    }
  }

  public static class Handler {
    private final String address;
    private final String path;
    private final String method;
    private Timers.EventTiming timer;

    Handler(String address, String path, String method) {
      this.address = address;
      this.path = path;
      this.method = method;
    }
  }
}
