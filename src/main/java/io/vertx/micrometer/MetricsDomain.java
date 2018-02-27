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
package io.vertx.micrometer;

import io.vertx.codegen.annotations.VertxGen;


/**
 * Metric domains with their associated prefixes.
 */
@VertxGen
public enum MetricsDomain {

  /**
   * Net server metrics.
   */
  NET_SERVER("vertx.net.server."),
  /**
   * Net client metrics.
   */
  NET_CLIENT("vertx.net.client."),
  /**
   * Http server metrics.
   */
  HTTP_SERVER("vertx.http.server."),
  /**
   * Http client metrics.
   */
  HTTP_CLIENT("vertx.http.client."),
  /**
   * Datagram socket metrics.
   */
  DATAGRAM_SOCKET("vertx.datagram."),
  /**
   * Event bus metrics.
   */
  EVENT_BUS("vertx.eventbus."),
  /**
   * Named pools metrics.
   */
  NAMED_POOLS("vertx.pool."),
  /**
   * Verticle metrics.
   */
  VERTICLES("vertx.verticle.");

  private String prefix;

  MetricsDomain(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
}
