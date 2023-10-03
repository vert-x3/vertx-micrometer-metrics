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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.EnumSet;
import java.util.concurrent.atomic.LongAdder;

import static io.vertx.micrometer.Label.*;

/**
 * @author Joel Takvorian
 */
class VertxNetClientMetrics extends AbstractMetrics {

  VertxNetClientMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    this(registry, MetricsDomain.NET_CLIENT, names, longGauges, enabledLabels);
  }

  VertxNetClientMetrics(MeterRegistry registry, MetricsDomain domain, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, domain, longGauges, enabledLabels);
  }

  TCPMetrics<?> forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance implements MicrometerMetrics, TCPMetrics<NetClientSocketMetric> {

    final Tags local;

    Instance(String localAddress) {
      local = toTags(LOCAL, s -> s == null ? "?" : s, localAddress);
    }

    @Override
    public NetClientSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
      Tags tags = local.and(toTags(REMOTE, Labels::address, remoteAddress, remoteName));
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
      counter(names.getNetErrorCount())
        .description("Number of errors")
        .tags(socketMetric.tags.and(toTags(CLASS_NAME, Class::getSimpleName, t.getClass())))
        .register(registry)
        .increment();
    }

    @Override
    public MeterRegistry registry() {
      return registry;
    }

    @Override
    public String baseName() {
      return VertxNetClientMetrics.this.baseName();
    }
  }

  class NetClientSocketMetric {

    final Tags tags;

    final LongAdder connections;
    final Counter bytesReceived;
    final Counter bytesSent;

    NetClientSocketMetric(Tags tags) {
      this.tags = tags;
      connections = longGauge(names.getNetActiveConnections())
        .description("Number of connections to the remote host currently opened")
        .tags(tags)
        .register(registry);
      bytesReceived = counter(names.getNetBytesRead())
        .description("Number of bytes received from the remote host")
        .tags(tags)
        .register(registry);
      bytesSent = counter(names.getNetBytesWritten())
        .description("Number of bytes sent to the remote host")
        .tags(tags)
        .register(registry);
    }
  }
}
