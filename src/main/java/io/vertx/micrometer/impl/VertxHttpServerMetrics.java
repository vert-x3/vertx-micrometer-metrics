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
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.Timer.Sample;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.impl.tags.Labels;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.HTTP_SERVER;

/**
 * @author Joel Takvorian
 */
class VertxHttpServerMetrics extends AbstractMetrics implements HttpServerMetrics<VertxHttpServerMetrics.RequestMetric, LongAdder> {

  private final Tags tcpLocal;
  private final Tags udpLocal;
  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;
  private final MeterProvider<Counter> requestResetCount;
  private final MeterProvider<DistributionSummary> requestBytes;
  private final MeterProvider<Counter> httpRequestsCount;
  private final MeterProvider<Timer> httpResponseTime;
  private final MeterProvider<DistributionSummary> httpResponseBytes;

  VertxHttpServerMetrics(AbstractMetrics parent, Function<HttpRequest, Iterable<Tag>> customTagsProvider,
                         String metricsName, SocketAddress tcpLocalAddress, SocketAddress udpLocalAddress) {
    super(parent, HTTP_SERVER);
    Tags base;
    if (enabledLabels.contains(SERVER_NAME)) {
      base = Tags.of(SERVER_NAME.toString(), metricsName == null ? "?" : metricsName);
    } else {
      base = Tags.empty();
    }
    if (enabledLabels.contains(LOCAL)) {
      tcpLocal = base.and(LOCAL.toString(), Labels.address(tcpLocalAddress));
      udpLocal = base.and(LOCAL.toString(), Labels.address(udpLocalAddress));
    } else {
      tcpLocal = base;
      udpLocal = base;
    }
    this.customTagsProvider = customTagsProvider;
    requestResetCount = Counter.builder(names.getHttpRequestResetsCount())
      .description("Number of request resets")
      .withRegistry(registry);
    requestBytes = DistributionSummary.builder(names.getHttpRequestBytes())
      .description("Size of requests in bytes")
      .withRegistry(registry);
    httpRequestsCount = Counter.builder(names.getHttpRequestsCount())
      .description("Number of processed requests")
      .withRegistry(registry);
    httpResponseTime = Timer.builder(names.getHttpResponseTime())
      .description("Request processing time")
      .withRegistry(registry);
    httpResponseBytes = DistributionSummary.builder(names.getHttpResponseBytes())
      .description("Size of responses in bytes")
      .withRegistry(registry);
  }


  @Override
  public RequestMetric requestBegin(SocketAddress remoteAddress, HttpRequest request) {
    Tags tags = request.version() == HttpVersion.HTTP_3 ? udpLocal : tcpLocal;
    if (enabledLabels.contains(REMOTE)) {
      String remoteName = remoteAddress.hostName();
      if (remoteName == null) {
        remoteName = "_";
      }
      tags = tags.and(REMOTE.toString(), Labels.address(remoteAddress, remoteName));
    }
    if (enabledLabels.contains(HTTP_PATH)) {
      tags = tags.and(HTTP_PATH.toString(), HttpUtils.parsePath(request.uri()));
    }
    if (enabledLabels.contains(HTTP_METHOD)) {
      tags = tags.and(HTTP_METHOD.toString(), request.method().toString());
    }
    if (customTagsProvider != null) {
      tags = tags.and(customTagsProvider.apply(request));
    }
    RequestMetric requestMetric = new RequestMetric(tags);
    requestMetric.requests.increment();
    return requestMetric;
  }

  @Override
  public void requestReset(RequestMetric requestMetric) {
    requestResetCount.withTags(requestMetric.tags).increment();
    requestMetric.requests.decrement();
    requestMetric.requestReset();
  }

  @Override
  public void requestEnd(RequestMetric requestMetric, HttpRequest request, long bytesRead) {
    requestBytes.withTags(requestMetric.tags).record(bytesRead);
    if (requestMetric.requestEnded()) {
      requestMetric.requests.decrement();
    }
  }

  @Override
  public RequestMetric responsePushed(SocketAddress remoteAddress, HttpMethod method, String uri, HttpResponse response) {
    Tags tags = tcpLocal;
    if (enabledLabels.contains(HTTP_PATH)) {
      tags.and(HTTP_PATH.toString(), HttpUtils.parsePath(uri));
    }
    if (enabledLabels.contains(HTTP_METHOD)) {
      tags.and(HTTP_METHOD.toString(), method.toString());
    }
    RequestMetric requestMetric = new RequestMetric(tags);
    requestMetric.requests.increment();
    return requestMetric;
  }

  @Override
  public void responseEnd(RequestMetric requestMetric, HttpResponse response, long bytesWritten) {
    Tags responseTags = requestMetric.tags;
    if (enabledLabels.contains(HTTP_ROUTE)) {
      responseTags = responseTags.and(HTTP_ROUTE.toString(), requestMetric.getRoute());
    }
    if (enabledLabels.contains(HTTP_CODE)) {
      responseTags = responseTags.and(HTTP_CODE.toString(), String.valueOf(response.statusCode()));
    }
    httpRequestsCount.withTags(responseTags).increment();
    requestMetric.sample.stop(httpResponseTime.withTags(responseTags));
    httpResponseBytes.withTags(responseTags).record(bytesWritten);
    if (requestMetric.responseEnded()) {
      requestMetric.requests.decrement();
    }
  }

  @Override
  public LongAdder connected(HttpRequest request) {
    Tags tags = tcpLocal;
    if (enabledLabels.contains(REMOTE)) {
      String remoteName = request.remoteAddress().hostName();
      if (remoteName == null) {
        remoteName = "_";
      }
      tags = tags.and(REMOTE.toString(), Labels.address(request.remoteAddress(), remoteName));
    }
    LongAdder wsConnections = longGaugeBuilder(names.getHttpActiveWsConnections(), LongAdder::doubleValue)
      .description("Number of websockets currently opened")
      .tags(tags)
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

  class RequestMetric {

    final Tags tags;

    final LongAdder requests;
    final Sample sample;

    // a string for a single route, a list of string for multiple
    private Object routes;
    // tracks length of resulting routes string
    private int routesLength;
    private boolean responseEnded;
    private boolean requestEnded;
    private boolean reset;

    RequestMetric(Tags tags) {
      this.tags = tags;
      requests = longGaugeBuilder(names.getHttpActiveRequests(), LongAdder::doubleValue)
        .description("Number of requests being processed")
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
