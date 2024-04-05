/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.micrometer.impl.tags.Labels;

import static io.vertx.micrometer.Label.CLASS_NAME;
import static io.vertx.micrometer.Label.LOCAL;
import static io.vertx.micrometer.MetricsDomain.DATAGRAM_SOCKET;

/**
 * @author Joel Takvorian
 */
class VertxDatagramSocketMetrics extends AbstractMetrics implements DatagramSocketMetrics {

  private final DistributionSummary bytesWritten;
  private final MeterProvider<Counter> errorCount;
  private volatile DistributionSummary bytesRead;

  VertxDatagramSocketMetrics(AbstractMetrics parent) {
    super(parent, DATAGRAM_SOCKET);
    bytesWritten = DistributionSummary.builder(names.getDatagramBytesWritten())
      .description("Total number of datagram bytes sent")
      .register(registry);
    errorCount = Counter.builder(names.getDatagramErrorCount())
      .description("Total number of datagram errors")
      .withRegistry(registry);
  }

  @Override
  public void listening(String localName, SocketAddress localAddress) {
    Tags tags;
    if (enabledLabels.contains(LOCAL)) {
      tags = Tags.of(LOCAL.toString(), Labels.address(localAddress, localName));
    } else {
      tags = Tags.empty();
    }
    bytesRead = DistributionSummary.builder(names.getDatagramBytesRead())
      .description("Total number of datagram bytes received")
      .tags(tags)
      .register(registry);
  }

  @Override
  public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    if (bytesRead != null) {
      bytesRead.record(numberOfBytes);
    }
  }

  @Override
  public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    bytesWritten.record(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
    Tags tags;
    if (enabledLabels.contains(CLASS_NAME)) {
      tags = Tags.of(CLASS_NAME.toString(), t.getClass().getSimpleName());
    } else {
      tags = Tags.empty();
    }
    errorCount.withTags(tags).increment();
  }
}
