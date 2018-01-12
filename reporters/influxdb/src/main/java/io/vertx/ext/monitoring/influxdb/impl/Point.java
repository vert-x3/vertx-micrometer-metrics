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

import io.vertx.codegen.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Representation of a InfluxDB database Point.
 *
 * @author stefan.majer [at] gmail.com
 */
public class Point {
  private String measurement;
  private Map<String, String> tags;
  private Long time;
  private TimeUnit precision = TimeUnit.NANOSECONDS;
  private Map<String, Object> fields;

  private static final int MAX_FRACTION_DIGITS = 340;

  Point() {
  }

  /**
   * Create a new Point Build build to create a new Point in a fluent manner.
   *
   * @param measurement the name of the measurement.
   * @return the Builder to be able to add further Builder calls.
   */

  public static Builder measurement(final String measurement) {
    return new Builder(measurement);
  }

  /**
   * Builder for a new Point.
   *
   * @author stefan.majer [at] gmail.com
   */
  public static final class Builder {
    private final String measurement;
    private final Map<String, String> tags = new TreeMap<>();
    private Long time;
    private TimeUnit precision = TimeUnit.NANOSECONDS;
    private final Map<String, Object> fields = new TreeMap<>();

    /**
     * @param measurement
     */
    Builder(final String measurement) {
      this.measurement = measurement;
    }

    /**
     * Add a tag to this point.
     *
     * @param tagName the tag name
     * @param value   the tag value
     * @return the Builder instance.
     */
    public Builder tag(final String tagName, final String value) {
      checkArgument(tagName != null);
      checkArgument(value != null);
      if (!tagName.isEmpty() && !value.isEmpty()) {
        tags.put(tagName, value);
      }
      return this;
    }

    /**
     * Add a Map of tags to add to this point.
     *
     * @param tagsToAdd the Map of tags to add
     * @return the Builder instance.
     */
    public Builder tag(final Map<String, String> tagsToAdd) {
      for (Entry<String, String> tag : tagsToAdd.entrySet()) {
        tag(tag.getKey(), tag.getValue());
      }
      return this;
    }

    /**
     * Add a field to this point.
     *
     * @param field the field name
     * @param value the value of this field
     * @return the Builder instance.
     */
    @SuppressWarnings("checkstyle:finalparameters")
    @Deprecated
    public Builder field(final String field, Object value) {
      if (value instanceof Number) {
        if (value instanceof Byte) {
          value = ((Byte) value).doubleValue();
        }
        if (value instanceof Short) {
          value = ((Short) value).doubleValue();
        }
        if (value instanceof Integer) {
          value = ((Integer) value).doubleValue();
        }
        if (value instanceof Long) {
          value = ((Long) value).doubleValue();
        }
        if (value instanceof BigInteger) {
          value = ((BigInteger) value).doubleValue();
        }

      }
      fields.put(field, value);
      return this;
    }

    public Builder addField(final String field, final boolean value) {
      fields.put(field, value);
      return this;
    }

    public Builder addField(final String field, final long value) {
      fields.put(field, value);
      return this;
    }

    public Builder addField(final String field, final double value) {
      fields.put(field, value);
      return this;
    }

    public Builder addField(final String field, final Number value) {
      fields.put(field, value);
      return this;
    }

    public Builder addField(final String field, final String value) {
      if (value == null) {
        throw new IllegalArgumentException("Field value cannot be null");
      }

      fields.put(field, value);
      return this;
    }

    /**
     * Add a Map of fields to this point.
     *
     * @param fieldsToAdd the fields to add
     * @return the Builder instance.
     */
    public Builder fields(final Map<String, Object> fieldsToAdd) {
      this.fields.putAll(fieldsToAdd);
      return this;
    }

    /**
     * Add a time to this point.
     *
     * @param precisionToSet
     * @param timeToSet
     * @return the Builder instance.
     */
    public Builder time(final long timeToSet, final TimeUnit precisionToSet) {
      checkNotNull(precisionToSet, "Precision must be not null!");
      this.time = timeToSet;
      this.precision = precisionToSet;
      return this;
    }

    /**
     * Create a new Point.
     *
     * @return the newly created Point.
     */
    public Point build() {
      checkArgument(!(this.measurement == null || this.measurement.isEmpty()), "Point name must not be null or empty. Was: " + this.measurement);
      checkArgument(this.fields.size() > 0, "Point must have at least one field specified.");
      Point point = new Point();
      point.setFields(this.fields);
      point.setMeasurement(this.measurement);
      if (this.time != null) {
        point.setTime(this.time);
        point.setPrecision(this.precision);
      } else {
        point.setTime(System.currentTimeMillis());
        point.setPrecision(TimeUnit.MILLISECONDS);
      }
      point.setTags(this.tags);
      return point;
    }
  }

  /**
   * @param measurement the measurement to set
   */
  void setMeasurement(final String measurement) {
    this.measurement = measurement;
  }

  /**
   * @param time the time to set
   */
  void setTime(final Long time) {
    this.time = time;
  }

  /**
   * @param tags the tags to set
   */
  void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  /**
   * @return the tags
   */
  Map<String, String> getTags() {
    return this.tags;
  }

  /**
   * @param precision the precision to set
   */
  void setPrecision(final TimeUnit precision) {
    this.precision = precision;
  }

  /**
   * @param fields the fields to set
   */
  void setFields(final Map<String, Object> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Point point = (Point) o;
    return Objects.equals(measurement, point.measurement)
      && Objects.equals(tags, point.tags)
      && Objects.equals(time, point.time)
      && precision == point.precision
      && Objects.equals(fields, point.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measurement, tags, time, precision, fields);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Point [name=");
    builder.append(this.measurement);
    builder.append(", time=");
    builder.append(this.time);
    builder.append(", tags=");
    builder.append(this.tags);
    builder.append(", precision=");
    builder.append(this.precision);
    builder.append(", fields=");
    builder.append(this.fields);
    builder.append("]");
    return builder.toString();
  }

  /**
   * calculate the lineprotocol entry for a single Point.
   * <p>
   * Documentation is WIP : https://github.com/influxdb/influxdb/pull/2997
   * <p>
   * https://github.com/influxdb/influxdb/blob/master/tsdb/README.md
   *
   * @return the String without newLine.
   */
  public String lineProtocol() {
    final StringBuilder sb = new StringBuilder();
    sb.append(escapeKey(this.measurement));
    sb.append(concatenatedTags());
    sb.append(concatenateFields());
    sb.append(formatedTime());
    return sb.toString();
  }

  private StringBuilder concatenatedTags() {
    final StringBuilder sb = new StringBuilder();
    for (Entry<String, String> tag : this.tags.entrySet()) {
      sb.append(",")
        .append(escapeKey(tag.getKey()))
        .append("=")
        .append(escapeKey(tag.getValue()));
    }
    sb.append(" ");
    return sb;
  }

  private StringBuilder concatenateFields() {
    final StringBuilder sb = new StringBuilder();
    final int fieldCount = this.fields.size();
    int loops = 0;

    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    numberFormat.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
    numberFormat.setGroupingUsed(false);
    numberFormat.setMinimumFractionDigits(1);

    for (Entry<String, Object> field : this.fields.entrySet()) {
      loops++;
      Object value = field.getValue();
      if (value == null) {
        continue;
      }

      sb.append(escapeKey(field.getKey())).append("=");

      if (value instanceof String) {
        String stringValue = (String) value;
        sb.append("\"").append(stringValue.replaceAll("\\", "\\\\").replaceAll("\"", "\\\"")).append("\"");
      } else if (value instanceof Number) {
        if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
          sb.append(numberFormat.format(value));
        } else {
          sb.append(value).append("i");
        }
      } else {
        sb.append(value);
      }

      if (loops < fieldCount) {
        sb.append(",");
      }
    }

    return sb;
  }

  private String escapeKey(String key) {
    return key.replaceAll(" ", "\\ ").replaceAll(",", "\\,").replaceAll("=", "\\=");
  }

  private StringBuilder formatedTime() {
    final StringBuilder sb = new StringBuilder();
    sb.append(" ").append(TimeUnit.NANOSECONDS.convert(this.time, this.precision));
    return sb;
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if {@code expression} is false
   */
  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression   a boolean expression
   * @param errorMessage the exception message to use if the check fails; will be converted to a
   *                     string using {@link String#valueOf(Object)}
   * @throws IllegalArgumentException if {@code expression} is false
   */
  public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference    an object reference
   * @param errorMessage the exception message to use if the check fails; will be converted to a
   *                     string using {@link String#valueOf(Object)}
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }
}
