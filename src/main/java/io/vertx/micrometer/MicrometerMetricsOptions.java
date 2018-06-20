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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Vert.x micrometer configuration.<br/>
 * It is required to set either {@code influxDbOptions}, {@code prometheusOptions} or {@code jmxMetricsOptions}
 * (or, programmatically, {@code micrometerRegistry})
 * in order to actually report metrics.
 *
 * @author Joel Takvorian
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class MicrometerMetricsOptions extends MetricsOptions {

  /**
   * Default registry name is 'default'
   */
  public static final String DEFAULT_REGISTRY_NAME = "default";

  /**
   * Default label match for public http server: exclude remote label
   */
  public static final List<Label> DEFAULT_LABELS = Arrays.asList(Label.HTTP_METHOD, Label.HTTP_CODE, Label.POOL_TYPE, Label.EB_SIDE);

  private Set<MetricsDomain> disabledMetricsCategories;
  private String registryName;
  private Set<Label> labels;
  private List<Match> labelMatches;
  private MeterRegistry micrometerRegistry;
  private VertxInfluxDbOptions influxDbOptions;
  private VertxPrometheusOptions prometheusOptions;
  private VertxJmxMetricsOptions jmxMetricsOptions;

  /**
   * Creates default options for Micrometer metrics.
   */
  public MicrometerMetricsOptions() {
    disabledMetricsCategories = EnumSet.noneOf(MetricsDomain.class);
    registryName = DEFAULT_REGISTRY_NAME;
    labels = EnumSet.copyOf(DEFAULT_LABELS);
    labelMatches = new ArrayList<>();
  }

  /**
   * Creates new options object for Micrometer metrics, which is a copy of {@code other}.
   */
  public MicrometerMetricsOptions(MicrometerMetricsOptions other) {
    super(other);
    disabledMetricsCategories = other.disabledMetricsCategories != null ? EnumSet.copyOf(other.disabledMetricsCategories) : EnumSet.noneOf(MetricsDomain.class);
    registryName = other.registryName;
    labels = other.labels != null ? EnumSet.copyOf(other.labels) : EnumSet.noneOf(Label.class);
    labelMatches = new ArrayList<>(other.labelMatches);
    micrometerRegistry = other.micrometerRegistry;
    if (other.influxDbOptions != null) {
      influxDbOptions = new VertxInfluxDbOptions(other.influxDbOptions);
    }
    if (other.prometheusOptions != null) {
      prometheusOptions = new VertxPrometheusOptions(other.prometheusOptions);
    }
    if (other.jmxMetricsOptions != null) {
      jmxMetricsOptions = new VertxJmxMetricsOptions(other.jmxMetricsOptions);
    }
  }

  /**
   * Creates new options object for Micrometer metrics from {@code json} input.
   */
  public MicrometerMetricsOptions(JsonObject json) {
    this();
    MicrometerMetricsOptionsConverter.fromJson(json, this);
    labelMatches = loadLabelMatches(json);
  }

  private List<Match> loadLabelMatches(JsonObject json) {
    List<Match> list = new ArrayList<>();

    JsonArray monitored = json.getJsonArray("labelMatches", new JsonArray());
    monitored.forEach(object -> {
      if (object instanceof JsonObject) list.add(new Match((JsonObject) object));
    });

    return list;
  }

  /**
   * Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
   */
  @Override
  public MicrometerMetricsOptions setEnabled(boolean enable) {
    super.setEnabled(enable);
    return this;
  }

  /**
   * @return the disabled metrics types.
   */
  public Set<MetricsDomain> getDisabledMetricsCategories() {
    return disabledMetricsCategories;
  }

  /**
   * Sets metrics types that are disabled.
   *
   * @param disabledMetricsCategories to specify the set of metrics types to be disabled.
   * @return a reference to this, so that the API can be used fluently
   */
  public MicrometerMetricsOptions setDisabledMetricsCategories(Set<MetricsDomain> disabledMetricsCategories) {
    this.disabledMetricsCategories = disabledMetricsCategories;
    return this;
  }

  /**
   * Set metric that will not be registered. Schedulers will check the set {@code disabledMetricsCategories} when
   * registering metrics suppliers
   *
   * @param metricsDomain the type of metrics
   * @return a reference to this, so that the API can be used fluently
   */
  @GenIgnore
  public MicrometerMetricsOptions addDisabledMetricsCategory(MetricsDomain metricsDomain) {
    if (disabledMetricsCategories == null) {
      disabledMetricsCategories = EnumSet.noneOf(MetricsDomain.class);
    }
    this.disabledMetricsCategories.add(metricsDomain);
    return this;
  }

  /**
   * Is the given metrics category disabled?
   * @return true if it is disabled
   */
  @GenIgnore
  public boolean isMetricsCategoryDisabled(MetricsDomain metricsDomain) {
    return disabledMetricsCategories != null && disabledMetricsCategories.contains(metricsDomain);
  }

  /**
   * Get the metrics registry name set in these options
   */
  public String getRegistryName() {
    return registryName;
  }

  /**
   * Set a name for the metrics registry, so that a new registry will be created and associated with this name.
   * If {@code registryName} is not provided (or null), a default registry will be used.
   * If the same name is given to several Vert.x instances (within the same JVM), they will share the same registry.
   * @param registryName a name to uniquely identify this registry
   */
  public MicrometerMetricsOptions setRegistryName(String registryName) {
    this.registryName = registryName;
    return this;
  }

  /**
   * @return the enabled labels.
   */
  public Set<Label> getLabels() {
    return labels;
  }

  /**
   * Sets enabled labels. These labels can be fine-tuned later on using Micrometer's Meter filters (see http://micrometer.io/docs/concepts#_meter_filters)
   *
   * @param labels the set of enabled labels - this set will replace any previously enabled labels, including the default ones
   * @return a reference to this, so that the API can be used fluently
   */
  public MicrometerMetricsOptions setLabels(Set<Label> labels) {
    this.labels = labels;
    return this;
  }

  /**
   * Add a labels to enable. These labels can be fine-tuned later on using Micrometer's Meter filters (see http://micrometer.io/docs/concepts#_meter_filters)
   *
   * @param labels the labels to enable
   * @return a reference to this, so that the API can be used fluently
   */
  @GenIgnore
  public MicrometerMetricsOptions addLabels(Label... labels) {
    if (this.labels == null) {
      this.labels = EnumSet.noneOf(Label.class);
    }
    this.labels.addAll(Arrays.asList(labels));
    return this;
  }

  /**
   * @return the list of label matching rules
   */
  public List<Match> getLabelMatches() {
    return labelMatches;
  }

  /**
   * Set a list of rules for label matching.
   *
   * @param matches the new list of rules
   * @return a reference to this, so the API can be used fluently
   */
  public MicrometerMetricsOptions setLabelMatches(List<Match> matches) {
    labelMatches = new ArrayList<>(matches);
    return this;
  }

  /**
   * Add a rule for label matching.
   *
   * @param match the label match
   * @return a reference to this, so the API can be used fluently
   */
  public MicrometerMetricsOptions addLabelMatch(Match match) {
    labelMatches.add(match);
    return this;
  }

  /**
   * Get the Micrometer MeterRegistry to be used by Vert.x, that has been previously set programmatically
   *
   * @return the micrometer registry.
   */
  public MeterRegistry getMicrometerRegistry() {
    return micrometerRegistry;
  }

  /**
   * Programmatically set the Micrometer MeterRegistry to be used by Vert.x.
   *
   * This is useful in several scenarios, such as:
   * <ul>
   *   <li>if there is already a MeterRegistry used in the application
   * that should be used by Vert.x as well.</li>
   *   <li>to define some backend configuration that is not covered in this module
   * (example: reporting to non-covered backends such as New Relic)</li>
   *   <li>to use Micrometer's CompositeRegistry</li>
   * </ul>
   *
   * This setter is mutually exclusive with setInfluxDbOptions/setPrometheusOptions/setJmxMetricsOptions
   * and takes precedence over them.
   *
   * @param micrometerRegistry the registry to use
   * @return a reference to this, so the API can be used fluently
   */
  public MicrometerMetricsOptions setMicrometerRegistry(MeterRegistry micrometerRegistry) {
    this.micrometerRegistry = micrometerRegistry;
    return this;
  }

  /**
   * Get the specific options for InfluxDB reporting.
   */
  public VertxInfluxDbOptions getInfluxDbOptions() {
    return influxDbOptions;
  }

  /**
   * Set InfluxDB options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param influxDbOptions backend options for InfluxDB
   */
  public MicrometerMetricsOptions setInfluxDbOptions(VertxInfluxDbOptions influxDbOptions) {
    this.influxDbOptions = influxDbOptions;
    return this;
  }

  /**
   * Get the specific options for Prometheus reporting.
   */
  public VertxPrometheusOptions getPrometheusOptions() {
    return prometheusOptions;
  }

  /**
   * Set Prometheus options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param prometheusOptions backend options for Prometheus
   */
  public MicrometerMetricsOptions setPrometheusOptions(VertxPrometheusOptions prometheusOptions) {
    this.prometheusOptions = prometheusOptions;
    return this;
  }

  /**
   * Get the specific options for JMX reporting.
   */
  public VertxJmxMetricsOptions getJmxMetricsOptions() {
    return jmxMetricsOptions;
  }

  /**
   * Set JMX metrics options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param jmxMetricsOptions backend options for JMX reporting
   */
  public MicrometerMetricsOptions setJmxMetricsOptions(VertxJmxMetricsOptions jmxMetricsOptions) {
    this.jmxMetricsOptions = jmxMetricsOptions;
    return this;
  }
}
