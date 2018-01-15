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

package io.vertx.ext.monitoring.collector.impl;

import java.util.concurrent.atomic.LongAdder;

/**
 * Holds measurements for all connections of a {@link io.vertx.core.net.NetClient} to a remote
 * {@link io.vertx.core.net.SocketAddress}.
 *
 * @author Thomas Segismont
 */
public class NetClientConnectionsMeasurements {
  // Connection info
  private final LongAdder connections = new LongAdder();
  // Bytes info
  private final LongAdder bytesReceived = new LongAdder();
  private final LongAdder bytesSent = new LongAdder();
  // Other
  private final LongAdder errorCount = new LongAdder();

  /**
   * Increment the number of opened connections.
   */
  public void incrementConnections() {
    connections.increment();
  }

  /**
   * Decrement the number of opened connections.
   */
  public void decrementConnections() {
    connections.decrement();
  }

  /**
   * @param numberOfBytes number of bytes to add to the received total
   */
  public void addBytesReceived(long numberOfBytes) {
    bytesReceived.add(numberOfBytes);
  }

  /**
   * @param numberOfBytes number of bytes to add to the sent total
   */
  public void addBytesSent(long numberOfBytes) {
    bytesSent.add(numberOfBytes);
  }

  /**
   * Increment the total number of errors.
   */
  public void incrementErrorCount() {
    errorCount.increment();
  }

  /**
   * @return a snaphsot of the current measurements
   */
  public Snapshot getSnapshot() {
    return new Snapshot(connections.sum(), bytesReceived.sum(), bytesSent.sum(), errorCount.sum());
  }

  /**
   * A snapshot of net client connections measurements at some point in time.
   */
  public static class Snapshot {
    private final long connections;
    private final long bytesReceived;
    private final long bytesSent;
    private final long errorCount;

    private Snapshot(long connections, long bytesReceived, long bytesSent, long errorCount) {
      this.connections = connections;
      this.bytesReceived = bytesReceived;
      this.bytesSent = bytesSent;
      this.errorCount = errorCount;
    }

    /**
     * @return number of opened connections
     */
    public long getConnections() {
      return connections;
    }

    /**
     * @return number of bytes received
     */
    public long getBytesReceived() {
      return bytesReceived;
    }

    /**
     * @return number of bytes sent
     */
    public long getBytesSent() {
      return bytesSent;
    }

    /**
     * @return total number of errors
     */
    public long getErrorCount() {
      return errorCount;
    }

    public static Snapshot merge(Snapshot s1, Snapshot s2) {
      return new Snapshot(s1.connections + s2.connections, s1.bytesReceived + s2.bytesReceived,
        s1.bytesSent + s2.bytesSent, s1.errorCount + s2.errorCount);
    }
  }
}
