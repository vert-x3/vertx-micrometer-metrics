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

/**
 * A gauge {@link DataPoint}. Gauge values may increase or decrease (e.g. room temperature).
 *
 * @author Thomas Segismont
 */
public class GaugePoint extends DataPoint {
  private final double value;

  public GaugePoint(String name, long timestamp, double value) {
    super(name, timestamp);
    this.value = value;
  }

  public Double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "GaugePoint{" + "name=" + getName() + ", timestamp=" + getTimestamp() + ", value=" + value + '}';
  }
}
