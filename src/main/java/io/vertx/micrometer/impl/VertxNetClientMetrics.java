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
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.impl.meters.Counters;
import io.vertx.micrometer.impl.meters.Gauges;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxNetClientMetrics extends AbstractMetrics {
  private final Gauges<LongAdder> connections;
  private final Counters bytesReceived;
  private final Counters bytesSent;
  private final Counters errorCount;

  VertxNetClientMetrics(MeterRegistry registry, MetricsNaming names, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    this(registry, MetricsDomain.NET_CLIENT, names, gaugesTable);
  }

  VertxNetClientMetrics(MeterRegistry registry, MetricsDomain domain, MetricsNaming names, ConcurrentMap<Meter.Id, Object> gaugesTable) {
    super(registry, domain, gaugesTable);
    connections = longGauges(names.getNetActiveConnections(), "Number of connections to the remote host currently opened", Label.LOCAL, Label.REMOTE);
    bytesReceived = counters(names.getNetBytesRead(), "Number of bytes received from the remote host", Label.LOCAL, Label.REMOTE);
    bytesSent = counters(names.getNetBytesWritten(), "Number of bytes sent to the remote host", Label.LOCAL, Label.REMOTE);
    errorCount = counters(names.getNetErrorCount(), "Number of errors", Label.LOCAL, Label.REMOTE, Label.CLASS_NAME);
  }

  TCPMetrics forAddress(String localAddress) {
    return new Instance(localAddress);
  }

  class Instance implements MicrometerMetrics, TCPMetrics<String> {
    protected final String local;

    Instance(String localAddress) {
      this.local = localAddress == null ? "?" : localAddress;
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
      bytesReceived.get(local, remote).increment(numberOfBytes);
    }

    @Override
    public void bytesWritten(String remote, SocketAddress remoteAddress, long numberOfBytes) {
      bytesSent.get(local, remote).increment(numberOfBytes);
    }

    @Override
    public void exceptionOccurred(String remote, SocketAddress remoteAddress, Throwable t) {
      errorCount.get(local, remote, t.getClass().getSimpleName()).increment();
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
      return VertxNetClientMetrics.this.baseName();
    }
  }
}
