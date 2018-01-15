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
 * An availability {@link DataPoint}. availabilities are used to indicate when a system goes up or down.
 *
 * @author Thomas Segismont
 */
public class AvailabilityPoint extends DataPoint {
  private final String value;

  public AvailabilityPoint(String name, long timestamp, String value) {
    super(name, timestamp);
    this.value = value;
  }

  public String getValue() {
    return value;
  }


  @Override
  public String toString() {
    return "AvailabilityPoint{" + "name=" + getName() + ", timestamp=" + getTimestamp() + ", value=" + value + '}';
  }
}
