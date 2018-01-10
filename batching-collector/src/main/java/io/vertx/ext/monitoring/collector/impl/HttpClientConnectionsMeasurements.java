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

import static java.util.concurrent.TimeUnit.*;

/**
 * Holds measurements for all connections of a {@link io.vertx.core.http.HttpClient} to a remote
 * {@link io.vertx.core.net.SocketAddress}.
 *
 * @author Thomas Segismont
 */
public class HttpClientConnectionsMeasurements {
  private final NetClientConnectionsMeasurements tcpMeasurements = new NetClientConnectionsMeasurements();
  private final LongAdder requests = new LongAdder();
  private final LongAdder requestCount = new LongAdder();
  private final LongAdder responseTime = new LongAdder();
  private final LongAdder wsConnections = new LongAdder();

  /**
   * Increment the number of opened connections.
   */
  public void incrementConnections() {
    tcpMeasurements.incrementConnections();
  }

  /**
   * Decrement the number of opened connections.
   */
  public void decrementConnections() {
    tcpMeasurements.decrementConnections();
  }

  /**
   * @param numberOfBytes number of bytes to add to the received total
   */
  public void addBytesReceived(long numberOfBytes) {
    tcpMeasurements.addBytesReceived(numberOfBytes);
  }

  /**
   * @param numberOfBytes number of bytes to add to the sent total
   */
  public void addBytesSent(long numberOfBytes) {
    tcpMeasurements.addBytesSent(numberOfBytes);
  }

  /**
   * Increment the total number of errors.
   */
  public void incrementErrorCount() {
    tcpMeasurements.incrementErrorCount();
  }

  /**
   * Signal a request has been sent.
   */
  public void requestBegin() {
    requests.increment();
    requestCount.increment();
  }

  /**
   * Signal a request couldn't complete successfully.
   */
  public void requestReset() {
    requests.decrement();
  }

  /**
   * Signal a response has been received.
   *
   * @param responseTime time elapsed until the response has been received, in nanoseconds
   */
  public void responseEnd(long responseTime) {
    requests.decrement();
    this.responseTime.add(responseTime);
  }

  /**
   * Increment the number of opened websocket connections.
   */
  public void incrementWsConnectionCount() {
    wsConnections.increment();
  }

  /**
   * Decrement the number of opened websocket connections.
   */
  public void decrementWsConnectionCount() {
    wsConnections.decrement();
  }

  /**
   * @return a snaphsot of the current measurements
   */
  public Snapshot getSnapshot() {
    return new Snapshot(tcpMeasurements.getSnapshot(), requests.sum(), requestCount.sum(), responseTime.sum(),
      wsConnections.sum());
  }

  /**
   * A snapshot of net client connections measurements at some point in time.
   */
  public static class Snapshot {
    private final NetClientConnectionsMeasurements.Snapshot tcpMeasurementsSnapshot;
    private final long requests;
    private final long requestCount;
    private final long responseTime;
    private final long wsConnections;

    private Snapshot(NetClientConnectionsMeasurements.Snapshot tcpMeasurementsSnapshot, long requests,
                     long requestCount, long responseTime, long wsConnections) {
      this.tcpMeasurementsSnapshot = tcpMeasurementsSnapshot;
      this.requests = requests;
      this.requestCount = requestCount;
      this.responseTime = responseTime;
      this.wsConnections = wsConnections;
    }

    /**
     * @return number of opened connections
     */
    public long getConnections() {
      return tcpMeasurementsSnapshot.getConnections();
    }

    /**
     * @return number of bytes received
     */
    public long getBytesReceived() {
      return tcpMeasurementsSnapshot.getBytesReceived();
    }

    /**
     * @return number of bytes sent
     */
    public long getBytesSent() {
      return tcpMeasurementsSnapshot.getBytesSent();
    }

    /**
     * @return total number of errors
     */
    public long getErrorCount() {
      return tcpMeasurementsSnapshot.getErrorCount();
    }

    /**
     * @return number of requests waiting for a response
     */
    public long getRequests() {
      return requests;
    }

    /**
     * @return total number of requests sent
     */
    public long getRequestCount() {
      return requestCount;
    }

    /**
     * @return cumulated response time
     */
    public long getResponseTime() {
      return MILLISECONDS.convert(responseTime, NANOSECONDS);
    }

    /**
     * @return number of opened websocket connections
     */
    public long getWsConnections() {
      return wsConnections;
    }

    public static Snapshot merge(Snapshot s1, Snapshot s2) {
      NetClientConnectionsMeasurements.Snapshot tcpMeasurementsSnapshot =
        NetClientConnectionsMeasurements.Snapshot.merge(s1.tcpMeasurementsSnapshot, s2.tcpMeasurementsSnapshot);
      return new Snapshot(tcpMeasurementsSnapshot, s1.requests + s2.requests, s1.requestCount + s2.requestCount,
        s1.responseTime + s2.responseTime, s1.wsConnections + s1.wsConnections);
    }
  }
}
