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
import io.micrometer.core.instrument.Tag;
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * @author Joel Takvorian
 */
class VertxHttpClientMetrics extends VertxNetClientMetrics {
  private final Timers queueDelay;
  private final Gauges<LongAdder> queueSize;
  private final Gauges<LongAdder> requests;
  private final Counters requestCount;
  private final Summaries requestBytes;
  private final Timers responseTime;
  private final Counters responseCount;
  private final Summaries responseBytes;
  private final Gauges<LongAdder> wsConnections;
  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;

  VertxHttpClientMetrics(MeterRegistry registry, MetricsNaming names, Function<HttpRequest, Iterable<Tag>> customTagsProvider, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.HTTP_CLIENT, names, gaugesTable);
    this.customTagsProvider = customTagsProvider;
    queueDelay = timers(names.getHttpQueueTime(), "Time spent in queue before being processed", Label.LOCAL, Label.REMOTE);
    queueSize = longGauges(names.getHttpQueuePending(), "Number of pending elements in queue", Label.LOCAL, Label.REMOTE);
    requests = longGauges(names.getHttpActiveRequests(), "Number of requests waiting for a response", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestCount = counters(names.getHttpRequestsCount(), "Number of requests sent", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestBytes = summaries(names.getHttpRequestBytes(), "Size of requests in bytes", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    responseTime = timers(names.getHttpResponseTime(), "Response time", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    responseCount = counters(names.getHttpResponsesCount(), "Response count with codes", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    responseBytes = summaries(names.getHttpResponseBytes(), "Size of responses in bytes", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    wsConnections = longGauges(names.getHttpActiveWsConnections(), "Number of websockets currently opened", Label.LOCAL, Label.REMOTE);
  }

  @Override
  HttpClientMetrics forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance extends VertxNetClientMetrics.Instance implements HttpClientMetrics<VertxHttpClientMetrics.Handler, String, String, Timers.EventTiming> {
    Instance(String localAddress) {
      super(localAddress);
    }

    @Override
    public ClientMetrics<Handler, Timers.EventTiming, HttpRequest, HttpResponse> createEndpointMetrics(SocketAddress remoteAddress, int maxPoolSize) {
      String remote = remoteAddress.toString();
      return new ClientMetrics<Handler, Timers.EventTiming, HttpRequest, HttpResponse>() {
        @Override
        public Timers.EventTiming enqueueRequest() {
          queueSize.get(local, remote).increment();
          return queueDelay.start();
        }

        @Override
        public void dequeueRequest(Timers.EventTiming taskMetric) {
          queueSize.get(local, remote).decrement();
          taskMetric.end(local, remote);
        }

        @Override
        public Handler requestBegin(String uri, HttpRequest request) {
          Handler handler = new Handler(remote, HttpUtils.parsePath(request.uri()), request.method().name());
          if (customTagsProvider != null) {
            handler.customTags = customTagsProvider.apply(request);
          }
          requests.get(handler.customTags, local, remote, handler.path, handler.method).increment();
          requestCount.get(handler.customTags, local, remote, handler.path, handler.method).increment();
          handler.timer = responseTime.start();
          return handler;
        }

        @Override
        public void requestEnd(Handler handler, long bytesWritten) {
          requestBytes.get(handler.customTags, local, handler.address, handler.path, handler.method).record(bytesWritten);
          if (handler.requestEnded()) {
            requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
          }
        }

        @Override
        public void requestReset(Handler handler) {
          requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
          handler.requestReset();
        }

        @Override
        public void responseBegin(Handler requestMetric, HttpResponse response) {
          requestMetric.response = response;
        }

        @Override
        public void responseEnd(Handler handler, long bytesRead) {
          String code = String.valueOf(handler.response.statusCode());
          if (handler.responseEnded()) {
            requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
          }
          responseCount.get(handler.customTags, local, handler.address, handler.path, handler.method, code).increment();
          handler.timer.end(handler.customTags, local, handler.address, handler.path, handler.method, code);
          responseBytes.get(handler.customTags, local, handler.address, handler.path, handler.method, code).record(bytesRead);
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
    HttpResponse response;
    private Iterable<Tag> customTags;
    private boolean responseEnded;
    private boolean requestEnded;
    private boolean reset;

    Handler(String address, String path, String method) {
      this.address = address;
      this.path = path;
      this.method = method;
    }

    void requestReset() {
      reset = true;
    }

    boolean requestEnded() {
      requestEnded = true;
      return !reset && responseEnded;
    }

    boolean responseEnded() {
      responseEnded = true;
      return !reset && requestEnded;
    }
  }
}
