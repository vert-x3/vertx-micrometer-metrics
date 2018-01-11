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
package io.vertx.ext.monitoring.common;

import io.vertx.codegen.annotations.VertxGen;


/**
 * Metrics types for each metrics.
 */
@VertxGen
public enum MetricsCategory {

  /**
   * Net server metrics.
   */
  NET_SERVER,
  /**
   * Net client metrics.
   */
  NET_CLIENT,
  /**
   * Http server metrics.
   */
  HTTP_SERVER,
  /**
   * Http client metrics.
   */
  HTTP_CLIENT,
  /**
   * Datagram socket metrics.
   */
  DATAGRAM_SOCKET,
  /**
   * Event bus metrics.
   */
  EVENT_BUS,
  /**
   * Named pools metrics.
   */
  NAMED_POOLS,
  /**
   * Verticle metrics.
   */
  VERTICLES
}
