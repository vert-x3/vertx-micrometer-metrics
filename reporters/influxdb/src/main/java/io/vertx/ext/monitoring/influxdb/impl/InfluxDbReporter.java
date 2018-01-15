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

package io.vertx.ext.monitoring.influxdb.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.monitoring.influxdb.AuthenticationOptions;
import io.vertx.ext.monitoring.influxdb.VertxInfluxDbOptions;
import io.vertx.ext.monitoring.collector.impl.AvailabilityPoint;
import io.vertx.ext.monitoring.collector.impl.CounterPoint;
import io.vertx.ext.monitoring.collector.impl.DataPoint;
import io.vertx.ext.monitoring.collector.impl.GaugePoint;
import io.vertx.ext.monitoring.collector.impl.HttpReporterBase;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.*;

public class InfluxDbReporter extends HttpReporterBase<VertxInfluxDbOptions> {
  private static final Logger LOG = LoggerFactory.getLogger(InfluxDbReporter.class);

  private static final CharSequence MEDIA_TYPE_TEXT_PLAIN = HttpHeaders.createOptimized("text/plain");

  private final String metricsDataUri;
  private final String prefix;

  private final CharSequence auth;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx InflxuDb options
   * @param context the metric collection and sending execution context
   */
  public InfluxDbReporter(Vertx vertx, VertxInfluxDbOptions options, Context context) {
    super(vertx, options, context, options.getHttpHeaders());

    metricsDataUri = options.getMetricsServiceUri() + "?db=" + options.getDatabase();
    prefix = options.getPrefix();

    AuthenticationOptions authenticationOptions = options.getAuthenticationOptions();
    if (authenticationOptions.isEnabled()) {
      String authString = authenticationOptions.getUsername() + ":" + authenticationOptions.getSecret();
      try {
        auth = HttpHeaders.createOptimized("Basic " + Base64.getEncoder().encodeToString(authString.getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      auth = null;
    }

    onContextInit(options);
  }

  @Override
  protected HttpClientOptions createHttpClientOptions(VertxInfluxDbOptions options) {
    return options.getHttpOptions()
      .setDefaultHost(options.getHost())
      .setDefaultPort(options.getPort());
  }

  @Override
  protected void getMetricsDataUri(Handler<AsyncResult<String>> handler) {
    handler.handle(Future.succeededFuture(metricsDataUri));
  }

  @Override
  protected void doSend(String metricsDataUri, List<DataPoint> dataPoints) {
    Optional<BatchPoints> optional = toBatchPoints(dataPoints);
    optional.ifPresent(b -> {
      HttpClientRequest request = httpClient.post(metricsDataUri, this::onResponse)
        .exceptionHandler(t -> handleException(b, t))
        .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_TEXT_PLAIN);

      if (auth != null) {
        request.putHeader(HttpHeaders.AUTHORIZATION, auth);
      }
      httpHeaders.forEach(request::putHeader);
      String lineProtocol = b.lineProtocol();
      if (LOG.isTraceEnabled()) {
        LOG.trace("Sending data to influxDb: \n" + lineProtocol);
      }

      request.end(lineProtocol, "UTF-8");
    });
  }

  private Optional<BatchPoints> toBatchPoints(List<DataPoint> dataPoints) {
    BatchPoints batchPoints = BatchPoints.builder()
      .tag("async", "true")
      .build();

    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
    mixedData = dataPoints.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));
    addMixedData(batchPoints, "gauges", mixedData.get(GaugePoint.class));
    addMixedData(batchPoints, "counters", mixedData.get(CounterPoint.class));
    addMixedData(batchPoints, "availabilities", mixedData.get(AvailabilityPoint.class));
    if (batchPoints.getPoints().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(batchPoints);
  }

  private void addMixedData(BatchPoints batchPoints, String type, Map<String, List<DataPoint>> data) {
    if (data == null || data.isEmpty()) {
      return;
    }
    data.forEach((id, points) -> points.forEach(point -> batchPoints.getPoints().add(toPoint(point))));
  }

  private Point toPoint(DataPoint dataPoint) {
    Point.Builder pointBuilder = Point.measurement(prefix)
      .time(dataPoint.getTimestamp(), TimeUnit.MILLISECONDS);
    if (dataPoint instanceof CounterPoint) {
      pointBuilder.addField(dataPoint.getName(), ((CounterPoint) dataPoint).getValue());
    } else if (dataPoint instanceof GaugePoint) {
      pointBuilder.addField(dataPoint.getName(), ((GaugePoint) dataPoint).getValue());
    } else {
      pointBuilder.addField(dataPoint.getName(), dataPoint.getValue().toString());
    }

    return pointBuilder.build();
  }
}
