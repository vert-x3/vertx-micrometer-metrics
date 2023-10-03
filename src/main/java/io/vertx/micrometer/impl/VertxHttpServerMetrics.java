/*
 * Copyright (c) 2011-2023 The original author or authors
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

import io.micrometer.core.instrument.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static io.vertx.micrometer.Label.*;
import static java.util.function.Function.identity;

/**
 * @author Joel Takvorian
 */
class VertxHttpServerMetrics extends VertxNetServerMetrics {

  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;

  VertxHttpServerMetrics(MeterRegistry registry, MetricsNaming names, Function<HttpRequest, Iterable<Tag>> customTagsProvider, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, MetricsDomain.HTTP_SERVER, longGauges, enabledLabels);
    this.customTagsProvider = customTagsProvider;
  }

  @Override
  HttpServerMetrics<?, ?, ?> forAddress(SocketAddress localAddress) {
    return new Instance(Labels.address(localAddress));
  }

  class Instance extends VertxNetServerMetrics.Instance implements HttpServerMetrics<RequestMetric, LongAdder, NetServerSocketMetric> {

    Instance(String local) {
      super(local);
    }

    @Override
    public RequestMetric requestBegin(NetServerSocketMetric socketMetric, HttpRequest request) {
      Tags tags = socketMetric.tags
        .and(toTags(HTTP_PATH, HttpRequest::uri, request, HTTP_METHOD, r -> r.method().toString(), request))
        .and(customTagsProvider == null ? Tags.empty() : customTagsProvider.apply(request));
      RequestMetric requestMetric = new RequestMetric(tags);
      requestMetric.requests.increment();
      return requestMetric;
    }

    @Override
    public void requestReset(RequestMetric requestMetric) {
      requestMetric.requestResetCount.increment();
      requestMetric.requests.decrement();
      requestMetric.requestReset();
    }

    @Override
    public void requestEnd(RequestMetric requestMetric, HttpRequest request, long bytesRead) {
      requestMetric.requestBytes.record(bytesRead);
      if (requestMetric.requestEnded()) {
        requestMetric.requests.decrement();
      }
    }

    @Override
    public RequestMetric responsePushed(NetServerSocketMetric socketMetric, HttpMethod method, String uri, HttpResponse response) {
      Tags tags = socketMetric.tags
        .and(toTags(HTTP_PATH, identity(), uri, HTTP_METHOD, HttpMethod::toString, method));
      RequestMetric requestMetric = new RequestMetric(tags);
      requestMetric.requests.increment();
      return requestMetric;
    }

    @Override
    public void responseEnd(RequestMetric requestMetric, HttpResponse response, long bytesWritten) {
      Tags responseTags = requestMetric.tags.and(toTags(HTTP_ROUTE, RequestMetric::getRoute, requestMetric, HTTP_CODE, r -> String.valueOf(r.statusCode()), response));
      counter(names.getHttpRequestsCount())
        .description("Number of processed requests")
        .tags(responseTags)
        .register(registry)
        .increment();
      requestMetric.sample.stop(timer(names.getHttpResponseTime())
        .description("Request processing time")
        .tags(responseTags)
        .register(registry));
      distributionSummary(names.getHttpResponseBytes())
        .description("Size of responses in bytes")
        .tags(responseTags)
        .register(registry)
        .record(bytesWritten);
      if (requestMetric.responseEnded()) {
        requestMetric.requests.decrement();
      }
    }

    @Override
    public LongAdder connected(NetServerSocketMetric socketMetric, RequestMetric requestMetric, ServerWebSocket serverWebSocket) {
      LongAdder wsConnections = longGauge(names.getHttpActiveWsConnections())
        .description("Number of websockets currently opened")
        .tags(socketMetric.tags)
        .register(registry);
      wsConnections.increment();
      return wsConnections;
    }

    @Override
    public void disconnected(LongAdder wsConnections) {
      wsConnections.decrement();
    }

    @Override
    public void requestRouted(RequestMetric requestMetric, String route) {
      requestMetric.addRoute(route);
    }
  }

  class RequestMetric {

    final Tags tags;

    final LongAdder requests;
    final Counter requestResetCount;
    final DistributionSummary requestBytes;
    final Timer.Sample sample;

    // a string for a single route, a list of string for multiple
    private Object routes;
    // tracks length of resulting routes string
    private int routesLength;
    private boolean responseEnded;
    private boolean requestEnded;
    private boolean reset;

    RequestMetric(Tags tags) {
      this.tags = tags;
      requests = longGauge(names.getHttpActiveRequests())
        .description("Number of requests being processed")
        .tags(tags)
        .register(registry);
      requestResetCount = counter(names.getHttpRequestResetsCount())
        .description("Number of request resets")
        .tags(tags)
        .register(registry);
      requestBytes = distributionSummary(names.getHttpRequestBytes())
        .description("Size of requests in bytes")
        .tags(tags)
        .register(registry);
      sample = Timer.start();
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
    }
  }
}
