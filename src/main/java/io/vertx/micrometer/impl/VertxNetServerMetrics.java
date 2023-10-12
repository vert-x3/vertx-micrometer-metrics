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
import io.vertx.micrometer.impl.VertxNetServerMetrics.NetServerSocketMetric;
import io.vertx.micrometer.impl.tags.Labels;
import io.vertx.micrometer.impl.tags.TagsWrapper;

import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;
import static io.vertx.micrometer.MetricsDomain.NET_SERVER;
import static io.vertx.micrometer.impl.tags.TagsWrapper.of;

/**
 * @author Joel Takvorian
 */
class VertxNetServerMetrics extends AbstractMetrics implements TCPMetrics<NetServerSocketMetric> {

  final TagsWrapper local;

  VertxNetServerMetrics(AbstractMetrics parent, SocketAddress localAddress) {
    this(parent, NET_SERVER, localAddress);
  }

  VertxNetServerMetrics(AbstractMetrics parent, MetricsDomain domain, SocketAddress localAddress) {
    super(parent, domain);
    local = of(toTag(LOCAL, Labels::address, localAddress));
  }


  @Override
  public NetServerSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
    TagsWrapper tags = local.and(toTag(REMOTE, Labels::address, remoteAddress, remoteName));
    NetServerSocketMetric socketMetric = new NetServerSocketMetric(tags);
    socketMetric.connections.increment();
    return socketMetric;
  }

  @Override
  public void disconnected(NetServerSocketMetric socketMetric, SocketAddress remoteAddress) {
    socketMetric.connections.decrement();
  }

  @Override
  public void bytesRead(NetServerSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    socketMetric.bytesReceived.increment(numberOfBytes);
  }

  @Override
  public void bytesWritten(NetServerSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    socketMetric.bytesSent.increment(numberOfBytes);
  }

  @Override
  public void exceptionOccurred(NetServerSocketMetric socketMetric, SocketAddress remoteAddress, Throwable t) {
    counter(names.getNetErrorCount(), "Number of errors", socketMetric.tags.and(toTag(CLASS_NAME, Class::getSimpleName, t.getClass())).unwrap())
      .increment();
  }

  class NetServerSocketMetric {

    final TagsWrapper tags;

    final LongAdder connections;
    final Counter bytesReceived;
    final Counter bytesSent;

    NetServerSocketMetric(TagsWrapper tags) {
      this.tags = tags;
      connections = longGauge(names.getNetActiveConnections(), "Number of opened connections to the server", tags.unwrap());
      bytesReceived = counter(names.getNetBytesRead(), "Number of bytes received by the server", tags.unwrap());
      bytesSent = counter(names.getNetBytesWritten(), "Number of bytes sent by the server", tags.unwrap());
    }
  }
}
