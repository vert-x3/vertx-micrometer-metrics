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
import io.vertx.core.http.WebSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static io.vertx.micrometer.Label.*;

/**
 * @author Joel Takvorian
 */
class VertxHttpClientMetrics extends VertxNetClientMetrics {

  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;

  VertxHttpClientMetrics(MeterRegistry registry, MetricsNaming names, Function<HttpRequest, Iterable<Tag>> customTagsProvider, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, MetricsDomain.HTTP_CLIENT, names, longGauges, enabledLabels);
    this.customTagsProvider = customTagsProvider;
  }

  @Override
  HttpClientMetrics<?, ?, ?, ?> forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance extends VertxNetClientMetrics.Instance implements HttpClientMetrics<RequestMetric, LongAdder, NetClientSocketMetric, Timer.Sample> {

    Instance(String localAddress) {
      super(localAddress);
    }

    @Override
    public ClientMetrics<RequestMetric, Timer.Sample, HttpRequest, HttpResponse> createEndpointMetrics(SocketAddress remoteAddress, int maxPoolSize) {
      Tags endPointTags = local.and(toTags(REMOTE, Labels::address, remoteAddress));
      return new EndpointMetrics(endPointTags);
    }

    @Override
    public LongAdder connected(WebSocket webSocket) {
      LongAdder wsConnections = longGauge(names.getHttpActiveWsConnections())
        .description("Number of websockets currently opened")
        .tags(local.and(toTags(REMOTE, Labels::address, webSocket.remoteAddress())))
        .register(registry);
      wsConnections.increment();
      return wsConnections;
    }

    @Override
    public void disconnected(LongAdder wsConnections) {
      wsConnections.decrement();
    }
  }

  class EndpointMetrics implements ClientMetrics<RequestMetric, Timer.Sample, HttpRequest, HttpResponse> {

    final Tags endPointTags;

    final Timer queueDelay;
    final LongAdder queueSize;

    EndpointMetrics(Tags endPointTags) {
      this.endPointTags = endPointTags;
      queueDelay = timer(names.getHttpQueueTime())
        .description("Time spent in queue before being processed")
        .tags(this.endPointTags)
        .register(registry);
      queueSize = longGauge(names.getHttpQueuePending())
        .description("Number of pending elements in queue")
        .tags(this.endPointTags)
        .register(registry);
    }

    @Override
    public Timer.Sample enqueueRequest() {
      queueSize.increment();
      return Timer.start();
    }

    @Override
    public void dequeueRequest(Timer.Sample taskMetric) {
      queueSize.decrement();
      taskMetric.stop(queueDelay);
    }

    @Override
    public RequestMetric requestBegin(String uri, HttpRequest request) {
      Tags tags = endPointTags
        .and(toTags(HTTP_PATH, HttpRequest::uri, request, HTTP_METHOD, r -> r.method().toString(), request))
        .and(customTagsProvider == null ? Tags.empty() : customTagsProvider.apply(request));
      RequestMetric requestMetric = new RequestMetric(tags);
      requestMetric.requests.increment();
      requestMetric.requestCount.increment();
      return requestMetric;
    }

    @Override
    public void requestEnd(RequestMetric requestMetric, long bytesWritten) {
      requestMetric.requestBytes.record(bytesWritten);
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
      requestMetric.responseCount.increment();
      requestMetric.sample.stop(requestMetric.responseTime);
      requestMetric.responseBytes.record(bytesRead);
    }

  }

  class RequestMetric {

    final Tags tags;

    final LongAdder requests;
    final Counter requestCount;
    final DistributionSummary requestBytes;
    final Timer.Sample sample;

    boolean responseEnded;
    boolean requestEnded;
    boolean reset;

    Timer responseTime;
    Counter responseCount;
    DistributionSummary responseBytes;

    RequestMetric(Tags tags) {
      this.tags = tags;
      requests = longGauge(names.getHttpActiveRequests())
        .description("Number of requests waiting for a response")
        .tags(tags)
        .register(registry);
      requestCount = counter(names.getHttpRequestsCount())
        .description("Number of requests sent")
        .tags(tags)
        .register(registry);
      requestBytes = distributionSummary(names.getHttpRequestBytes())
        .description("Size of requests in bytes")
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

    public void responseBegin(HttpResponse response) {
      Tags responseTags = tags.and(toTags(HTTP_CODE, r -> String.valueOf(r.statusCode()), response));
      responseTime = timer(names.getHttpResponseTime())
        .description("Response time")
        .tags(responseTags)
        .register(registry);
      responseCount = counter(names.getHttpResponsesCount())
        .description("Response count with codes")
        .tags(responseTags)
        .register(registry);
      responseBytes = distributionSummary(names.getHttpResponseBytes())
        .description("Size of responses in bytes")
        .tags(responseTags)
        .register(registry);
    }

    boolean responseEnded() {
      responseEnded = true;
      return !reset && requestEnded;
    }
  }
}
