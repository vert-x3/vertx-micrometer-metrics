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
 * A counter {@link DataPoint}. Counters are used when we are more interested in changes of the values, rather than in
 * the value themselves (e.g. number of requests processed since startup).
 *
 * @author Thomas Segismont
 */
public class CounterPoint extends DataPoint {
  private final long value;

  public CounterPoint(String name, long timestamp, long value) {
    super(name, timestamp);
    this.value = value;
  }

  public Long getValue() {
    return value;
  }


  @Override
  public String toString() {
    return "CounterPoint{" + "name=" + getName() + ", timestamp=" + getTimestamp() + ", value=" + value + '}';
  }
}
