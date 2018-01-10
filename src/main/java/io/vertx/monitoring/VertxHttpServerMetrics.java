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
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Counters;
import io.vertx.monitoring.meters.Gauges;
import io.vertx.monitoring.meters.Timers;

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

  VertxHttpServerMetrics(LabelMatchers labelMatchers, MeterRegistry registry) {
    super(labelMatchers, registry, MetricsCategory.HTTP_SERVER, "vertx.http.server.");
    requests = longGauges("requests", "Number of requests being processed", Labels.LOCAL, Labels.REMOTE, Labels.PATH);
    requestCount = counters("requestCount", "Number of processed requests", Labels.LOCAL, Labels.REMOTE, Labels.PATH, Labels.METHOD, Labels.CODE);
    requestResetCount = counters("requestResetCount", "Number of requests reset", Labels.LOCAL, Labels.REMOTE, Labels.PATH);
    processingTime = timers("responseTime", "Request processing time", Labels.LOCAL, Labels.REMOTE, Labels.PATH);
    wsConnections = longGauges("wsConnections", "Number of websockets currently opened", Labels.LOCAL, Labels.REMOTE);
  }

  @Override
  HttpServerMetrics forAddress(SocketAddress localAddress) {
    String local = Labels.fromAddress(localAddress);
    return new Instance(local);
  }

  class Instance extends VertxNetServerMetrics.Instance implements HttpServerMetrics<Handler, String, String> {
    Instance(String local) {
      super(local);
    }

    @Override
    public Handler requestBegin(String remote, HttpServerRequest request) {
      Handler handler = new Handler(remote, request.path(), request.method().name());
      requests.get(labelMatchers, local, remote, handler.path).increment();
      handler.timer = processingTime.start(labelMatchers, local, remote, handler.path);
      return handler;
    }

    @Override
    public void requestReset(Handler handler) {
      requestResetCount.get(labelMatchers, local, handler.address, handler.path).increment();
      requests.get(labelMatchers, local, handler.address, handler.path).decrement();
    }

    @Override
    public Handler responsePushed(String remote, HttpMethod method, String uri, HttpServerResponse response) {
      Handler handler = new Handler(remote, uri, method.name());
      requests.get(labelMatchers, local, remote, handler.path).increment();
      return handler;
    }

    @Override
    public void responseEnd(Handler handler, HttpServerResponse response) {
      handler.timer.end();
      requestCount.get(labelMatchers, local, handler.address, handler.path, handler.method, String.valueOf(response.getStatusCode())).increment();
      requests.get(labelMatchers, local, handler.address, handler.path).decrement();
    }

    @Override
    public String upgrade(Handler handler, ServerWebSocket serverWebSocket) {
      return handler.address;
    }

    @Override
    public String connected(String remote, ServerWebSocket serverWebSocket) {
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
    private final String method;
    private Timers.EventTiming timer;

    Handler(String address, String path, String method) {
      this.address = address;
      this.path = path;
      this.method = method;
    }
  }
}
