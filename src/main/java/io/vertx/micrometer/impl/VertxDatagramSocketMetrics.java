/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Summaries;

import java.util.concurrent.ConcurrentMap;

/**
 * @author Joel Takvorian
 */
class VertxDatagramSocketMetrics extends AbstractMetrics implements DatagramSocketMetrics {
  private final Summaries bytesReceived;
  private final Summaries bytesSent;
  private final Counters errorCount;

  private volatile String localAddress;

  VertxDatagramSocketMetrics(MeterRegistry registry, MetricsNaming names, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, MetricsDomain.DATAGRAM_SOCKET, gaugesTable);
    bytesReceived = summaries(names.getDatagramBytesRead(), "Total number of datagram bytes received", Label.LOCAL);
    bytesSent = summaries(names.getDatagramBytesWritten(), "Total number of datagram bytes sent");
    errorCount = counters(names.getDatagramErrorCount(), "Total number of datagram errors", Label.CLASS_NAME);
  }

  @Override
  public void listening(String localName, SocketAddress localAddress) {
    this.localAddress = Labels.address(localAddress, localName);
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    if (localAddress != null) {
      bytesReceived.get(localAddress).record(numberOfBytes);
    }
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    bytesSent.get().record(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    errorCount.get(t.getClass().getSimpleName()).increment();
  }

  @Override
  public void close() {
  }
}
