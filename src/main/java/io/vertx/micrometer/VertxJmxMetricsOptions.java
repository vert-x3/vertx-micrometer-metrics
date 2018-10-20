/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.micrometer;

import io.micrometer.jmx.JmxConfig;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.time.Duration;

/**
 * Options for Prometheus metrics backend.
 *
 * @author Joel Takvorian
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxJmxMetricsOptions {

  /**
   * Default value for enabled = false.
   */
  public static final boolean DEFAULT_ENABLED = false;

  /**
   * Default value for the domain = metrics.
   */
  public static final String DEFAULT_DOMAIN = "metrics";

  /**
   * Default value for metric collection interval (in seconds) = 10.
   */
  public static final int DEFAULT_STEP = 10;

  private boolean enabled;
  private String domain;
  private int step;

  /**
   * Default constructor
   */
  public VertxJmxMetricsOptions() {
    enabled = DEFAULT_ENABLED;
    domain = DEFAULT_DOMAIN;
    step = DEFAULT_STEP;
  }

  /**
   * Copy constructor
   *
   * @param other The other {@link VertxJmxMetricsOptions} to copy when creating this
   */
  public VertxJmxMetricsOptions(VertxJmxMetricsOptions other) {
    enabled = other.enabled;
    domain = other.domain;
    step = other.step;
  }

  /**
   * Create an instance from a {@link JsonObject}
   *
   * @param json the JsonObject to create it from
   */
  public VertxJmxMetricsOptions(JsonObject json) {
    this();
    VertxJmxMetricsOptionsConverter.fromJson(json, this);
  }

  /**
   * @return a JSON representation of these options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    VertxJmxMetricsOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Will JMX reporting be enabled?
   *
   * @return true if enabled, false if not.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set true to enable Prometheus reporting
   */
  public VertxJmxMetricsOptions setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get the JMX domain under which metrics are published
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Set the JMX domain under which to publish metrics
   */
  public VertxJmxMetricsOptions setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * Get the step of push intervals, in seconds
   */
  public int getStep() {
    return step;
  }

  /**
   * Push interval steps, in seconds. Default is 10 seconds.
   */
  public VertxJmxMetricsOptions setStep(int step) {
    this.step = step;
    return this;
  }

  /**
   * Convert these options to a Micrometer's {@code JmxConfig} object
   */
  public JmxConfig toMicrometerConfig() {
    return new JmxConfig() {
      @Override
      public String get(String s) {
        return null;
      }

      @Override
      public String domain() {
        return domain;
      }

      @Override
      public Duration step() {
        return Duration.ofSeconds(step);
      }
    };

  }

}
