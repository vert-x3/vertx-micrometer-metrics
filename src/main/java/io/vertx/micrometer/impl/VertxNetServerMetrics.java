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
class VertxNetServerMetrics extends AbstractMetrics {

  VertxNetServerMetrics(MeterRegistry registry, MetricsNaming names, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    this(registry, names, MetricsDomain.NET_SERVER, longGauges, enabledLabels);
  }

  VertxNetServerMetrics(MeterRegistry registry, MetricsNaming names, MetricsDomain domain, LongGauges longGauges, EnumSet<Label> enabledLabels) {
    super(registry, names, domain, longGauges, enabledLabels);
  }

  TCPMetrics<?> forAddress(SocketAddress localAddress) {
    return new Instance(Labels.address(localAddress));
  }

  class Instance implements MicrometerMetrics, TCPMetrics<NetServerSocketMetric> {

    final Tags local;

    Instance(String localAddress) {
      local = toTags(LOCAL, s -> s == null ? "?" : s, localAddress);
    }

    @Override
    public NetServerSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
      Tags tags = local.and(toTags(REMOTE, Labels::address, remoteAddress, remoteName));
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
      return VertxNetServerMetrics.this.baseName();
    }
  }

  class NetServerSocketMetric {

    final Tags tags;

    final LongAdder connections;
    final Counter bytesReceived;
    final Counter bytesSent;

    NetServerSocketMetric(Tags tags) {
      this.tags = tags;
      connections = longGauge(names.getNetActiveConnections())
        .description("Number of opened connections to the server")
        .tags(tags)
        .register(registry);
      bytesReceived = counter(names.getNetBytesRead())
        .description("Number of bytes received by the server")
        .tags(tags)
        .register(registry);
      bytesSent = counter(names.getNetBytesWritten())
        .description("Number of bytes sent by the server")
        .tags(tags)
        .register(registry);
    }
  }
}
