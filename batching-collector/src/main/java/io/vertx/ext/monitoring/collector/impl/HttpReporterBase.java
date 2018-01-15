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
package io.vertx.ext.monitoring.collector.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.monitoring.collector.BatchingReporterOptions;
import io.vertx.ext.monitoring.collector.Reporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;

/**
 * Base class for reporters sending metrics data over HTTP.
 *
 * @author Thomas Segismont
 * @author Dan Kristensen
 */
public abstract class HttpReporterBase<T extends BatchingReporterOptions> implements Reporter {
  private static final Logger LOG = LoggerFactory.getLogger(HttpReporterBase.class);

  protected final Vertx vertx;
  private final Context context;

  private final int batchSize;
  private final long batchDelay;
  private final List<DataPoint> queue;

  protected final Map<CharSequence, Iterable<CharSequence>> httpHeaders;

  private long timerId;
  private long sendTime;

  protected HttpClient httpClient;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Extended metrics options
   * @param context the metric collection and sending execution context
   * @param httpHeaders custom HTTP headers to send with metrics data
   */
  public HttpReporterBase(Vertx vertx, T options, Context context, JsonObject httpHeaders) {
    this.vertx = vertx;
    this.context = context;
    batchSize = options.getBatchSize();
    batchDelay = NANOSECONDS.convert(options.getBatchDelay(), SECONDS);
    queue = new ArrayList<>(batchSize);
    if (httpHeaders != null) {
      this.httpHeaders = new HashMap<>(httpHeaders.size());
      for (String headerName : httpHeaders.fieldNames()) {
        CharSequence optimizedName = HttpHeaders.createOptimized(headerName);
        Object value = httpHeaders.getValue(headerName);
        List<String> values;
        if (value instanceof JsonArray) {
          values = ((JsonArray) value).stream().map(Object::toString).collect(toList());
        } else {
          values = Collections.singletonList(value.toString());
        }
        this.httpHeaders.put(optimizedName, values.stream().map(HttpHeaders::createOptimized).collect(toList()));
      }
    } else {
      this.httpHeaders = Collections.emptyMap();
    }
  }

  /**
   * To be invoked by subclasses after object is initialized.
   *
   * @param options the metrics implementation options
   */
  protected void onContextInit(T options) {
    context.runOnContext(aVoid -> {
      timerId = vertx.setPeriodic(MILLISECONDS.convert(batchDelay, NANOSECONDS), tid -> flushIfIdle());
      sendTime = System.nanoTime();
      httpClient = vertx.createHttpClient(createHttpClientOptions(options));
    });
  }

  private void flushIfIdle() {
    if (System.nanoTime() - sendTime > batchDelay && !queue.isEmpty()) {
      LOG.trace("Flushing queue with " + queue.size() + " elements");
      List<DataPoint> dataPoints = new ArrayList<>(queue);
      queue.clear();
      send(dataPoints);
    }
  }

  protected abstract HttpClientOptions createHttpClientOptions(T options);

  @Override
  public void handle(List<DataPoint> dataPoints) {
    if (LOG.isTraceEnabled()) {
      String lineSeparator = System.getProperty("line.separator");
      String msg = "Handling data points: " + lineSeparator +
        dataPoints.stream().map(DataPoint::toString).collect(joining(lineSeparator));
      LOG.trace(msg);
    }

    if (queue.size() + dataPoints.size() < batchSize) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Will queue datapoints. Queue size will be " + (queue.size() + dataPoints.size()));
      }
      queue.addAll(dataPoints);
      return;
    }
    LOG.trace("Prepare for sending datapoints");
    List<DataPoint> temp = new ArrayList<>(queue.size() + dataPoints.size());
    temp.addAll(queue);
    temp.addAll(dataPoints);
    queue.clear();
    do {
      List<DataPoint> subList = temp.subList(0, batchSize);
      send(subList);
      subList.clear();
    } while (temp.size() >= batchSize);
    queue.addAll(temp);
  }

  private void send(List<DataPoint> dataPoints) {
    getMetricsDataUri(ar -> {
      if (ar.succeeded()) {
        doSend(ar.result(), dataPoints);
        sendTime = System.nanoTime();
      }
    });
  }

  protected abstract void getMetricsDataUri(Handler<AsyncResult<String>> handler);

  protected abstract void doSend(String metricsDataUri, List<DataPoint> dataPoints);

  protected void onResponse(HttpClientResponse response) {
    if (response.statusCode() != 200 && LOG.isTraceEnabled()) {
      response.bodyHandler(msg -> {
        LOG.trace("Could not send metrics: " + response.statusCode() + " : " + msg.toString());
      });
    }
  }

  protected void handleException(Object payload, Throwable t) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Could not send metrics. Payload was: " + String.valueOf(payload), t);
    }
  }

  @Override
  public void stop() {
    vertx.cancelTimer(timerId);
    httpClient.close();
  }
}
