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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.VertxNetClientMetrics.NetClientSocketMetric;
import io.vertx.micrometer.impl.tags.Labels;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.NET_CLIENT;

/**
 * @author Joel Takvorian
 */
class VertxNetClientMetrics extends AbstractMetrics implements TCPMetrics<NetClientSocketMetric> {

  final Tags local;
  private final MeterProvider<Counter> netErrorCount;

  VertxNetClientMetrics(AbstractMetrics parent, String localAddress) {
    this(parent, NET_CLIENT, localAddress);
  }

  VertxNetClientMetrics(AbstractMetrics parent, MetricsDomain domain, String localAddress) {
    super(parent, domain);
    if (enabledLabels.contains(LOCAL)) {
      local = Tags.of(LOCAL.toString(), localAddress == null ? "?" : localAddress);
    } else {
      local = Tags.empty();
    }
    netErrorCount = Counter.builder(names.getNetErrorCount())
      .description("Number of errors")
      .withRegistry(registry);
  }

  @Override
  public NetClientSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
    Tags tags = local;
    if (enabledLabels.contains(REMOTE)) {
      tags = tags.and(REMOTE.toString(), Labels.address(remoteAddress, remoteName));
    }
    NetClientSocketMetric socketMetric = new NetClientSocketMetric(tags);
    socketMetric.connections.increment();
    return socketMetric;
  }

  @Override
  public void disconnected(NetClientSocketMetric socketMetric, SocketAddress remoteAddress) {
    socketMetric.connections.decrement();
  }

  @Override
  public void bytesRead(NetClientSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    socketMetric.bytesReceived.increment(numberOfBytes);
  }

  @Override
  public void bytesWritten(NetClientSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    socketMetric.bytesSent.increment(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(NetClientSocketMetric socketMetric, SocketAddress remoteAddress, Throwable t) {
    Tags tags = socketMetric.tags;
    if (enabledLabels.contains(CLASS_NAME)) {
      tags = tags.and(CLASS_NAME.toString(), t.getClass().getSimpleName());
    }
    netErrorCount.withTags(tags).increment();
  }

  class NetClientSocketMetric {

    final Tags tags;

    final LongAdder connections;
    final Counter bytesReceived;
    final Counter bytesSent;

    NetClientSocketMetric(Tags tags) {
      this.tags = tags;
      connections = longGaugeBuilder(names.getNetActiveConnections(), LongAdder::doubleValue)
        .description("Number of connections to the remote host currently opened")
        .tags(tags)
        .register(registry);
      bytesReceived = Counter.builder(names.getNetBytesRead())
        .description("Number of bytes received from the remote host")
        .tags(tags)
        .register(registry);
      bytesSent = Counter.builder(names.getNetBytesWritten())
        .description("Number of bytes sent to the remote host")
        .tags(tags)
        .register(registry);
    }
  }
}
