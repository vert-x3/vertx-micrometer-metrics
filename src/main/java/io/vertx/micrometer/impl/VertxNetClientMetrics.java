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
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.VertxNetClientMetrics.NetClientSocketMetric;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.NET_CLIENT;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;

/**
 * @author Joel Takvorian
 */
class VertxNetClientMetrics extends AbstractMetrics implements TCPMetrics<NetClientSocketMetric> {

  final TagsWrapper local;

  VertxNetClientMetrics(AbstractMetrics parent, String localAddress) {
    this(parent, NET_CLIENT, localAddress);
  }

  VertxNetClientMetrics(AbstractMetrics parent, MetricsDomain domain, String localAddress) {
    super(parent, domain);
    local = of(toTag(LOCAL, s -> s == null ? "?" : s, localAddress));
  }

  @Override
  public NetClientSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
    TagsWrapper tags = local.and(toTag(REMOTE, Labels::address, remoteAddress, remoteName));
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
    counter(names.getNetErrorCount(), "Number of errors", socketMetric.tags.and(toTag(CLASS_NAME, Class::getSimpleName, t.getClass())).unwrap())
      .increment();
  }

  class NetClientSocketMetric {

    final TagsWrapper tags;

    final LongAdder connections;
    final Counter bytesReceived;
    final Counter bytesSent;

    NetClientSocketMetric(TagsWrapper tags) {
      this.tags = tags;
      connections = longGauge(names.getNetActiveConnections(), "Number of connections to the remote host currently opened", tags.unwrap());
      bytesReceived = counter(names.getNetBytesRead(), "Number of bytes received from the remote host", tags.unwrap());
      bytesSent = counter(names.getNetBytesWritten(), "Number of bytes sent to the remote host", tags.unwrap());
    }
  }
}
