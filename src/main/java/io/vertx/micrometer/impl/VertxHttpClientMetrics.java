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
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.HTTP_CLIENT;

/**
 * @author Joel Takvorian
 */
class VertxHttpClientMetrics extends VertxNetClientMetrics implements HttpClientMetrics<RequestMetric, LongAdder, NetClientSocketMetric, Sample> {

  private final Function<HttpRequest, Iterable<Tag>> customTagsProvider;

  VertxHttpClientMetrics(AbstractMetrics parent, Function<HttpRequest, Iterable<Tag>> customTagsProvider, String localAddress) {
    super(parent, HTTP_CLIENT, localAddress);
    this.customTagsProvider = customTagsProvider == null ? r -> Tags.empty() : customTagsProvider;
  }

  @Override
  public ClientMetrics<RequestMetric, Sample, HttpRequest, HttpResponse> createEndpointMetrics(SocketAddress remoteAddress, int maxPoolSize) {
    TagsWrapper endPointTags = local.and(toTag(REMOTE, Labels::address, remoteAddress));
    return new EndpointMetrics(endPointTags);
  }

  @Override
  public LongAdder connected(WebSocket webSocket) {
    LongAdder wsConnections = longGauge(names.getHttpActiveWsConnections(), "Number of websockets currently opened", local.and(toTag(REMOTE, Labels::address, webSocket.remoteAddress())).unwrap());
    wsConnections.increment();
    return wsConnections;
  }

  @Override
  public void disconnected(LongAdder wsConnections) {
    wsConnections.decrement();
  }

  class EndpointMetrics implements ClientMetrics<RequestMetric, Sample, HttpRequest, HttpResponse> {

    final TagsWrapper endPointTags;

    final Timer queueDelay;
    final LongAdder queueSize;

    EndpointMetrics(TagsWrapper endPointTags) {
      this.endPointTags = endPointTags;
      queueDelay = timer(names.getHttpQueueTime(), "Time spent in queue before being processed", endPointTags.unwrap());
      queueSize = longGauge(names.getHttpQueuePending(), "Number of pending elements in queue", endPointTags.unwrap());
    }

    @Override
    public Sample enqueueRequest() {
      queueSize.increment();
      return Timer.start();
    }

    @Override
    public void dequeueRequest(Sample taskMetric) {
      queueSize.decrement();
      taskMetric.stop(queueDelay);
    }

    @Override
    public RequestMetric requestBegin(String uri, HttpRequest request) {
      TagsWrapper tags = endPointTags
        .and(toTag(HTTP_PATH, HttpRequest::uri, request), toTag(HTTP_METHOD, r -> r.method().toString(), request))
        .and(customTagsProvider.apply(request));
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

    final TagsWrapper tags;

    final LongAdder requests;
    final Counter requestCount;
    final DistributionSummary requestBytes;
    final Sample sample;

    boolean responseEnded;
    boolean requestEnded;
    boolean reset;

    Timer responseTime;
    Counter responseCount;
    DistributionSummary responseBytes;

    RequestMetric(TagsWrapper tags) {
      this.tags = tags;
      requests = longGauge(names.getHttpActiveRequests(), "Number of requests waiting for a response", tags.unwrap());
      requestCount = counter(names.getHttpRequestsCount(), "Number of requests sent", tags.unwrap());
      requestBytes = distributionSummary(names.getHttpRequestBytes(), "Size of requests in bytes", tags.unwrap());
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
      TagsWrapper responseTags = tags.and(toTag(HTTP_CODE, r -> String.valueOf(r.statusCode()), response));
      responseTime = timer(names.getHttpResponseTime(), "Response time", responseTags.unwrap());
      responseCount = counter(names.getHttpResponsesCount(), "Response count with codes", responseTags.unwrap());
      responseBytes = distributionSummary(names.getHttpResponseBytes(), "Size of responses in bytes", responseTags.unwrap());
    }

    boolean responseEnded() {
      responseEnded = true;
      return !reset && requestEnded;
    }
  }
}
