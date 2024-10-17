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
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.impl.VertxHttpClientMetrics.RequestMetric;
import io.vertx.micrometer.impl.VertxNetClientMetrics.NetClientSocketMetric;
import io.vertx.micrometer.impl.tags.Labels;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.HTTP_CLIENT;

/**
 * @author Joel Takvorian
 */
class VertxHttpClientMetrics extends VertxNetClientMetrics implements HttpClientMetrics<RequestMetric, LongAdder, NetClientSocketMetric> {

  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;
  private final MeterProvider<Counter> requestCount;
  private final MeterProvider<DistributionSummary> requestBytes;
  private final MeterProvider<Timer> responseTime;
  private final MeterProvider<Counter> responseCount;
  private final MeterProvider<DistributionSummary> responseBytes;

  VertxHttpClientMetrics(AbstractMetrics parent, Function<HttpRequest, Iterable<Tag>> customTagsProvider, String localAddress) {
    super(parent, HTTP_CLIENT, localAddress);
    this.customTagsProvider = customTagsProvider;
    requestCount = Counter.builder(names.getHttpRequestsCount())
      .description("Number of requests sent")
      .withRegistry(registry);
    requestBytes = DistributionSummary.builder(names.getHttpRequestBytes())
      .description("Size of requests in bytes")
      .withRegistry(registry);
    responseTime = Timer.builder(names.getHttpResponseTime())
      .description("Response time")
      .withRegistry(registry);
    responseCount = Counter.builder(names.getHttpResponsesCount())
      .description("Response count with codes")
      .withRegistry(registry);
    responseBytes = DistributionSummary.builder(names.getHttpResponseBytes())
      .description("Size of responses in bytes")
      .withRegistry(registry);
  }

  @Override
  public ClientMetrics<RequestMetric, HttpRequest, HttpResponse> createEndpointMetrics(SocketAddress remoteAddress, int maxPoolSize) {
    Tags endPointTags = local;
    if (enabledLabels.contains(REMOTE)) {
      endPointTags = endPointTags.and(REMOTE.toString(), Labels.address(remoteAddress));
    }
    return new EndpointMetrics(endPointTags);
  }

  @Override
  public LongAdder connected(WebSocket webSocket) {
    Tags tags = local;
    if (enabledLabels.contains(REMOTE)) {
      tags = tags.and(REMOTE.toString(), Labels.address(webSocket.remoteAddress()));
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

  class EndpointMetrics implements ClientMetrics<RequestMetric, HttpRequest, HttpResponse> {

    final Tags endPointTags;

    EndpointMetrics(Tags endPointTags) {
      this.endPointTags = endPointTags;
    }

    @Override
    public RequestMetric requestBegin(String uri, HttpRequest request) {
      Tags tags = endPointTags;
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
      requestCount.withTags(tags).increment();
      return requestMetric;
    }

    @Override
    public void requestEnd(RequestMetric requestMetric, long bytesWritten) {
      requestBytes.withTags(requestMetric.tags).record(bytesWritten);
      if (requestMetric.requestEnded()) {
        requestMetric.requests.decrement();
      }
    }

    @Override
    public void requestReset(RequestMetric requestMetric) {
      requestMetric.requests.decrement();
      requestMetric.requestReset();
    }

    @Override
    public void responseBegin(RequestMetric requestMetric, HttpResponse response) {
      requestMetric.responseBegin(response);
    }

    @Override
    public void responseEnd(RequestMetric requestMetric, long bytesRead) {
      if (requestMetric.responseEnded()) {
        requestMetric.requests.decrement();
      }
      responseCount.withTags(requestMetric.responseTags).increment();
      requestMetric.sample.stop(responseTime.withTags(requestMetric.responseTags));
      responseBytes.withTags(requestMetric.responseTags).record(bytesRead);
    }

  }

  class RequestMetric {

    final Tags tags;

    final LongAdder requests;
    final Sample sample;

    Tags responseTags;
    boolean responseEnded;
    boolean requestEnded;
    boolean reset;

    RequestMetric(Tags tags) {
      this.tags = tags;
      responseTags = tags;
      requests = longGaugeBuilder(names.getHttpActiveRequests(), LongAdder::doubleValue)
        .description("Number of requests waiting for a response")
        .tags(tags)
        .register(registry);
      sample = Timer.start();
    }

    void requestReset() {
      reset = true;
    }

    boolean requestEnded() {
      requestEnded = true;
      return !reset && responseEnded;
    }

    void responseBegin(HttpResponse response) {
      if (enabledLabels.contains(HTTP_CODE)) {
        responseTags = responseTags.and(HTTP_CODE.toString(), String.valueOf(response.statusCode()));
      }
    }

    boolean responseEnded() {
      responseEnded = true;
      return !reset && requestEnded;
    }
  }
}
