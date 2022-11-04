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
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;
import io.vertx.micrometer.impl.meters.Timers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * @author Joel Takvorian
 */
class VertxHttpServerMetrics extends VertxNetServerMetrics {
  private final Gauges<LongAdder> requests;
  private final Counters requestCount;
  private final Counters requestResetCount;
  private final Summaries requestBytes;
  private final Timers processingTime;
  private final Summaries responseBytes;
  private final Gauges<LongAdder> wsConnections;
  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;

  VertxHttpServerMetrics(MeterRegistry registry, MetricsNaming names, Function<HttpRequest, Iterable<Tag>> customTagsProvider, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.HTTP_SERVER, names, gaugesTable);
    this.customTagsProvider = customTagsProvider;
    requests = longGauges(names.getHttpActiveRequests(), "Number of requests being processed", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestCount = counters(names.getHttpRequestsCount(), "Number of processed requests", Label.LOCAL, Label.REMOTE, Label.HTTP_ROUTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    requestResetCount = counters(names.getHttpRequestResetsCount(), "Number of request resets", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    requestBytes = summaries(names.getHttpRequestBytes(), "Size of requests in bytes", Label.LOCAL, Label.REMOTE, Label.HTTP_PATH, Label.HTTP_METHOD);
    processingTime = timers(names.getHttpResponseTime(), "Request processing time", Label.LOCAL, Label.REMOTE, Label.HTTP_ROUTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    responseBytes = summaries(names.getHttpResponseBytes(), "Size of responses in bytes", Label.LOCAL, Label.REMOTE, Label.HTTP_ROUTE, Label.HTTP_PATH, Label.HTTP_METHOD, Label.HTTP_CODE);
    wsConnections = longGauges(names.getHttpActiveWsConnections(), "Number of websockets currently opened", Label.LOCAL, Label.REMOTE);
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
    public Handler requestBegin(String remote, HttpRequest request) {
      Handler handler = new Handler(remote, request.uri(), request.method().name());
      if (customTagsProvider != null) {
        handler.customTags = customTagsProvider.apply(request);
      }
      requests.get(handler.customTags, local, remote, handler.path, handler.method).increment();
      handler.timer = processingTime.start();
      return handler;
    }

    @Override
    public void requestReset(Handler handler) {
      requestResetCount.get(handler.customTags, local, handler.address, handler.path, handler.method).increment();
      requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
      handler.requestReset();
    }

    @Override
    public void requestEnd(Handler handler, HttpRequest request, long bytesRead) {
      requestBytes.get(handler.customTags, local, handler.address, handler.path, handler.method).record(bytesRead);
      if (handler.requestEnded()) {
        requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
      }
    }

    @Override
    public Handler responsePushed(String remote, HttpMethod method, String uri, HttpResponse response) {
      Handler handler = new Handler(remote, uri, method.name());
      requests.get(handler.customTags, local, remote, handler.path, handler.method).increment();
      return handler;
    }

    @Override
    public void responseBegin(Handler handler, HttpResponse response) {
    }

    @Override
    public void responseEnd(Handler handler, HttpResponse response, long bytesWritten) {
      String code = String.valueOf(response.statusCode());
      String handlerRoute = handler.getRoute();
      handler.timer.end(handler.customTags, local, handler.address, handlerRoute, handler.path, handler.method, code);
      requestCount.get(handler.customTags, local, handler.address, handlerRoute, handler.path, handler.method, code).increment();
      responseBytes.get(handler.customTags, local, handler.address, handlerRoute, handler.path, handler.method, code).record(bytesWritten);
      if (handler.responseEnded()) {
        requests.get(handler.customTags, local, handler.address, handler.path, handler.method).decrement();
      }
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
    public void requestRouted(Handler handler, String route) {
      handler.addRoute(route);
    }

    @Override
    public void close() {
    }
  }

  public static class Handler {
    private final String address;
    private final String path;
    private final String method;
    // a string for a single route, a list of string for multiple
    private Object routes;
    // tracks length of resulting routes string
    private int routesLength;
    private Timers.EventTiming timer;
    private Iterable<Tag> customTags;
    private boolean responseEnded;
    private boolean requestEnded;
    private boolean reset;

    Handler(String address, String path, String method) {
      this.address = address;
      this.path = path;
      this.method = method;
    }

    // we try to minimize allocations as far as possible. see https://github.com/vert-x3/vertx-dropwizard-metrics/pull/101
    private void addRoute(String route) {
      if (route == null) {
        return;
      }
      routesLength += route.length();
      if (routes == null) {
        routes = route;
        return;
      }
      ++routesLength;
      if (routes instanceof List) {
        //noinspection unchecked
        ((List<String>) routes).add(route);
        return;
      }
      List<String> multipleRoutes = new LinkedList<>();
      multipleRoutes.add((String) routes);
      multipleRoutes.add(route);
      routes = multipleRoutes;
    }

    private String getRoute() {
      if (routes == null) {
        return "";
      }
      if (routes instanceof String) {
        return (String) routes;
      }
      StringBuilder concatenation = new StringBuilder(routesLength);
      @SuppressWarnings("unchecked") Iterator<String> iterator = ((List<String>) routes).iterator();
      concatenation.append(iterator.next());
      while (iterator.hasNext()) {
        concatenation.append('>').append(iterator.next());
      }
      routes = concatenation.toString();
      return (String) routes;
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
    }  }
}
