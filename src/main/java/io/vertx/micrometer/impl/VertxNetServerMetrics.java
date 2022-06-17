/*
 * Copyright (c) 2011-2022 The original author or authors
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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;
import io.vertx.micrometer.impl.meters.Summaries;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxNetServerMetrics extends AbstractMetrics {
  private final Gauges<LongAdder> connections;
  private final Summaries bytesReceived;
  private final Summaries bytesSent;
  private final Counters errorCount;

  VertxNetServerMetrics(MeterRegistry registry, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    this(registry, MetricsDomain.NET_SERVER, gaugesTable);
  }

  VertxNetServerMetrics(MeterRegistry registry, MetricsDomain domain, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, domain, gaugesTable);
    connections = longGauges("connections", "Number of opened connections to the server", Label.LOCAL, Label.REMOTE);
    bytesReceived = summaries("bytesReceived", "Number of bytes received by the server", Label.LOCAL, Label.REMOTE);
    bytesSent = summaries("bytesSent", "Number of bytes sent by the server", Label.LOCAL, Label.REMOTE);
    errorCount = counters("errors", "Number of errors", Label.LOCAL, Label.REMOTE, Label.CLASS_NAME);
  }

  TCPMetrics forAddress(SocketAddress localAddress) {
    return new Instance(Labels.address(localAddress));
  }

  class Instance implements MicrometerMetrics, TCPMetrics<String> {
    final String local;

    Instance(String local) {
      this.local = local;
    }

    @Override
    public String connected(SocketAddress remoteAddress, String remoteName) {
      String remote = Labels.address(remoteAddress, remoteName);
      connections.get(local, remote).increment();
      return remote;
    }

    @Override
    public void disconnected(String remote, SocketAddress remoteAddress) {
      connections.get(local, remote).decrement();
    }

    @Override
    public void bytesRead(String remote, SocketAddress remoteAddress, long numberOfBytes) {
      bytesReceived.get(local, remote).record(numberOfBytes);
    }

    @Override
    public void bytesWritten(String remote, SocketAddress remoteAddress, long numberOfBytes) {
      bytesSent.get(local, remote).record(numberOfBytes);
    }

    @Override
    public void exceptionOccurred(String remote, SocketAddress remoteAddress, Throwable t) {
      errorCount.get(local, remote, t.getClass().getSimpleName()).increment();
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public void close() {
    }

    @Override
    public MeterRegistry registry() {
      return registry;
    }

    @Override
    public String baseName() {
      return domain.getPrefix();
    }
  }
}
