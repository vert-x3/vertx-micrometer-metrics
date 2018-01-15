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

import java.util.Objects;

/**
 * Base class for metric data points. Defines the metric name and timestamp of the point.
 *
 * @author Thomas Segismont
 */
public abstract class DataPoint {
  private final String name;
  private final long timestamp;

  public DataPoint(String name, long timestamp) {
    Objects.requireNonNull(name, "name");
    this.name = name;
    this.timestamp = timestamp;
  }

  public String getName() {
    return name;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public abstract Object getValue();
}
