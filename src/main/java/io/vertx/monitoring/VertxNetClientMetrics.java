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

package io.vertx.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.monitoring.match.LabelMatchers;
import io.vertx.monitoring.meters.Counters;
import io.vertx.monitoring.meters.Gauges;
import io.vertx.monitoring.meters.Summaries;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author Joel Takvorian
 */
class VertxNetClientMetrics extends AbstractMetrics {
  private final Gauges<LongAdder> connections;
  private final Summaries bytesReceived;
  private final Summaries bytesSent;
  private final Counters errorCount;

  VertxNetClientMetrics(LabelMatchers labelMatchers, MeterRegistry registry) {
    this(labelMatchers, registry, MetricsCategory.NET_CLIENT, "vertx.net.client.");
  }

  VertxNetClientMetrics(LabelMatchers labelMatchers, MeterRegistry registry, MetricsCategory domain, String baseName) {
    super(labelMatchers, registry, domain, baseName);
    connections = longGauges("connections", "Number of connections to the remote host currently opened", Labels.LOCAL, Labels.REMOTE);
    bytesReceived = summaries("bytesReceived", "Number of bytes received from the remote host", Labels.LOCAL, Labels.REMOTE);
    bytesSent = summaries("bytesSent", "Number of bytes sent to the remote host", Labels.LOCAL, Labels.REMOTE);
    errorCount = counters("errors", "Number of errors", Labels.LOCAL, Labels.REMOTE, Labels.CLASS);
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
      String remote = Labels.fromAddress(new SocketAddressImpl(remoteAddress.port(), remoteName));
      connections.get(labelMatchers, local, remote).increment();
      return remote;
    }

    @Override
    public void disconnected(String remote, SocketAddress remoteAddress) {
      connections.get(labelMatchers, local, remote).decrement();
    }

    @Override
    public void bytesRead(String remote, SocketAddress remoteAddress, long numberOfBytes) {
      bytesReceived.get(labelMatchers, local, remote).record(numberOfBytes);
    }

    @Override
    public void bytesWritten(String remote, SocketAddress remoteAddress, long numberOfBytes) {
      bytesSent.get(labelMatchers, local, remote).record(numberOfBytes);
    }

    @Override
    public void exceptionOccurred(String remote, SocketAddress remoteAddress, Throwable t) {
      errorCount.get(labelMatchers, local, remote, t.getClass().getSimpleName()).increment();
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
      return baseName;
    }
  }
}
