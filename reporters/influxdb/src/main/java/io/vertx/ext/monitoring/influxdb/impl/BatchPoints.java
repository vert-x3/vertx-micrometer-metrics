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

package io.vertx.ext.monitoring.influxdb.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * {Purpose of This Type}.
 * <p>
 * {Other Notes Relating to This Type (Optional)}
 *
 * @author stefan
 */
public class BatchPoints {
  private Map<String, String> tags;
  private List<Point> points;

  BatchPoints() {
    // Only visible in the Builder
  }

  /**
   * Create a new BatchPoints build to create a new BatchPoints in a fluent manner.
   *
   * @return the Builder to be able to add further Builder calls.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The Builder to create a new BatchPoints instance.
   */
  public static final class Builder {
    private final Map<String, String> tags = new TreeMap<>();
    private final List<Point> points = new ArrayList<>();

    Builder() {
    }

    /**
     * Add a tag to this set of points.
     *
     * @param tagName the tag name
     * @param value   the tag value
     * @return the Builder instance.
     */
    public Builder tag(final String tagName, final String value) {
      this.tags.put(tagName, value);
      return this;
    }

    /**
     * Add a Point to this set of points.
     *
     * @param pointToAdd
     * @return the Builder instance
     */
    public Builder point(final Point pointToAdd) {
      this.points.add(pointToAdd);
      return this;
    }

    /**
     * Add a set of Points to this set of points.
     *
     * @param pointsToAdd
     * @return the Builder instance
     */
    public Builder points(final Point... pointsToAdd) {
      this.points.addAll(Arrays.asList(pointsToAdd));
      return this;
    }

    /**
     * Create a new BatchPoints instance.
     *
     * @return the created BatchPoints.
     */
    public BatchPoints build() {
      BatchPoints batchPoints = new BatchPoints();
      for (Point point : this.points) {
        point.getTags().putAll(this.tags);
      }
      batchPoints.setPoints(this.points);
      batchPoints.setTags(this.tags);
      return batchPoints;
    }
  }

  /**
   * @return the points
   */
  public List<Point> getPoints() {
    return this.points;
  }

  /**
   * @param points the points to set
   */
  void setPoints(final List<Point> points) {
    this.points = points;
  }

  /**
   * Add a single Point to these batches.
   *
   * @param point
   * @return this Instance to be able to daisy chain calls.
   */
  public BatchPoints point(final Point point) {
    point.getTags().putAll(this.tags);
    this.points.add(point);
    return this;
  }

  /**
   * @return the tags
   */
  public Map<String, String> getTags() {
    return this.tags;
  }

  /**
   * @param tags the tags to set
   */
  void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchPoints that = (BatchPoints) o;
    return Objects.equals(tags, that.tags)
      && Objects.equals(points, that.points);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags, points);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("BatchPoints [tags=");
    builder.append(this.tags);
    builder.append(", points=");
    builder.append(this.points);
    builder.append("]");
    return builder.toString();
  }

  // measurement[,tag=value,tag2=value2...] field=value[,field2=value2...] [unixnano]

  /**
   * calculate the lineprotocol for all Points.
   *
   * @return the String with newLines.
   */
  public String lineProtocol() {
    StringBuilder sb = new StringBuilder();
    for (Point point : this.points) {
      sb.append(point.lineProtocol()).append("\n");
    }
    return sb.toString();
  }
}
