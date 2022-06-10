/*
 * Copyright (c) 2011-2022 The original author or authors
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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxHttpServerMetrics extends VertxNetServerMetrics {
  private final Gauges<LongAdder> requests;
  private final Counters requestCount;
  private final Counters requestResetCount;
  private final Timers processingTime;
  private final Gauges<LongAdder> wsConnections;

  VertxHttpServerMetrics(MeterRegistry registry, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.HTTP_SERVER, gaugesTable);
    requests = longGauges("requests", "Number of requests being processed", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestCount = counters("requestCount", "Number of processed requests", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    requestResetCount = counters("requestResetCount", "Number of requests reset", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    processingTime = timers("responseTime", "Request processing time", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    wsConnections = longGauges("wsConnections", "Number of websockets currently opened", Label.LOCAL, Label.REMOTE);
  }

  @Override
  HttpServerMetrics forAddress(SocketAddress localAddress) {
    return new Instance(Labels.address(localAddress));
  }

  class Instance extends VertxNetServerMetrics.Instance implements HttpServerMetrics<Handler, String, String> {
    Instance(String local) {
      super(local);
    }

    @Override
    public Handler requestBegin(String remote, HttpServerRequest request) {
      Handler handler = new Handler(remote, request.path(), request.method().name());
      requests.get(local, remote, handler.path, handler.method).increment();
      handler.timer = processingTime.start();
      return handler;
    }

    @Override
    public void requestReset(Handler handler) {
      requestResetCount.get(local, handler.address, handler.path, handler.method).increment();
      requests.get(local, handler.address, handler.path, handler.method).decrement();
    }

    @Override
    public Handler responsePushed(String remote, HttpMethod method, String uri, HttpServerResponse response) {
      Handler handler = new Handler(remote, uri, method.name());
      requests.get(local, remote, handler.path, handler.method).increment();
      return handler;
    }

    @Override
    public void responseEnd(Handler handler, HttpServerResponse response) {
      String code = String.valueOf(response.getStatusCode());
      handler.timer.end(local, handler.address, handler.path, handler.method, code);
      requestCount.get(local, handler.address, handler.path, handler.method, code).increment();
      requests.get(local, handler.address, handler.path, handler.method).decrement();
    }

    @Override
    public String connected(String socketMetric, Handler handler, ServerWebSocket serverWebSocket) {
      wsConnections.get(local, handler.address).increment();
      return handler.address;
    }

    @Override
    public void disconnected(String remote) {
      wsConnections.get(local, remote).decrement();
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
    private final String method;
    private Timers.EventTiming timer;

    Handler(String address, String path, String method) {
      this.address = address;
      this.path = path;
      this.method = method;
    }
  }
}
