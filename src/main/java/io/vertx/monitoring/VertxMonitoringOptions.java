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
package io.vertx.monitoring;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.monitoring.backend.VertxInfluxDbOptions;
import io.vertx.monitoring.backend.VertxJmxMetricsOptions;
import io.vertx.monitoring.backend.VertxPrometheusOptions;
import io.vertx.monitoring.match.Match;
import io.vertx.monitoring.match.MatchType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Vert.x monitoring configuration.<br/>
 * It is required to set either {@code influxDbOptions}, {@code prometheusOptions} or {@code jmxMetricsOptions]
 * in order to actually report metrics.
 *
 * @author Joel Takvorian
 */
@DataObject(generateConverter = true, inheritConverter = true)
public class VertxMonitoringOptions extends MetricsOptions {

  /**
   * Default registry name is 'default'
   */
  public static final String DEFAULT_REGISTRY_NAME = "default";

  /**
   * Default label match for public http server: exclude remote label
   */
  public static final Match DEFAULT_HTTP_SERVER_MATCH = new Match()
    .setDomain(MetricsDomain.HTTP_SERVER)
    .setLabel("remote")
    .setType(MatchType.REGEX)
    .setValue(".*")
    .setAlias("_");

  /**
   * Default label match for public net server: exclude remote label
   */
  public static final Match DEFAULT_NET_SERVER_MATCH = new Match()
    .setDomain(MetricsDomain.NET_SERVER)
    .setLabel("remote")
    .setType(MatchType.REGEX)
    .setValue(".*")
    .setAlias("_");

  /**
   * The default label matches: empty by default
   */
  public static final List<Match> DEFAULT_LABEL_MATCHES = Arrays.asList(DEFAULT_HTTP_SERVER_MATCH, DEFAULT_NET_SERVER_MATCH);

  private Set<MetricsDomain> disabledMetricsCategories;
  private String registryName;
  private List<Match> labelMatchs;
  private VertxInfluxDbOptions influxDbOptions;
  private VertxPrometheusOptions prometheusOptions;
  private VertxJmxMetricsOptions jmxMetricsOptions;

  public VertxMonitoringOptions() {
    disabledMetricsCategories = EnumSet.noneOf(MetricsDomain.class);
    registryName = DEFAULT_REGISTRY_NAME;
    labelMatchs = new ArrayList<>(DEFAULT_LABEL_MATCHES);
  }

  public VertxMonitoringOptions(VertxMonitoringOptions other) {
    super(other);
    disabledMetricsCategories = other.disabledMetricsCategories != null ? EnumSet.copyOf(other.disabledMetricsCategories) : EnumSet.noneOf(MetricsDomain.class);
    registryName = other.registryName;
    labelMatchs = new ArrayList<>(other.labelMatchs);
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

  public VertxMonitoringOptions(JsonObject json) {
    this();
    VertxMonitoringOptionsConverter.fromJson(json, this);
    labelMatchs = loadLabelMatches(json);
  }

  private List<Match> loadLabelMatches(JsonObject json) {
    List<Match> list = new ArrayList<>();

    JsonArray monitored = json.getJsonArray("labelMatchs", new JsonArray());
    monitored.forEach(object -> {
      if (object instanceof JsonObject) list.add(new Match((JsonObject) object));
    });

    return list;
  }

  /**
   * Set whether metrics will be enabled on the Vert.x instance. Metrics are not enabled by default.
   */
  @Override
  public VertxMonitoringOptions setEnabled(boolean enable) {
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
  public VertxMonitoringOptions setDisabledMetricsCategories(Set<MetricsDomain> disabledMetricsCategories) {
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
  public VertxMonitoringOptions addDisabledMetricsCategory(MetricsDomain metricsDomain) {
    if (disabledMetricsCategories == null) {
      disabledMetricsCategories = EnumSet.noneOf(MetricsDomain.class);
    }
    this.disabledMetricsCategories.add(metricsDomain);
    return this;
  }

  @GenIgnore
  public boolean isMetricsCategoryDisabled(MetricsDomain metricsDomain) {
    return disabledMetricsCategories != null && disabledMetricsCategories.contains(metricsDomain);
  }

  public String getRegistryName() {
    return registryName;
  }

  /**
   * Set a name for the metrics registry, so that a new registry will be created and associated with this name.
   * If {@code registryName} is not provided (or null), a default registry will be used.
   * If the same name is given to several Vert.x instances (within the same JVM), they will share the same registry.
   * @param registryName a name to uniquely identify this registry
   */
  public VertxMonitoringOptions setRegistryName(String registryName) {
    this.registryName = registryName;
    return this;
  }

  /**
   * @return the list of label matching rules
   */
  public List<Match> getLabelMatchs() {
    return labelMatchs;
  }

  /**
   * Add a rule for label matching.
   *
   * @param match the label match
   * @return a reference to this, so the API can be used fluently
   */
  public VertxMonitoringOptions addLabelMatch(Match match) {
    labelMatchs.add(match);
    return this;
  }

  /**
   * Reset the label matching rules, so that there is no default behaviour
   *
   * @return a reference to this, so the API can be used fluently
   */
  public VertxMonitoringOptions resetLabelMatchs() {
    labelMatchs.clear();
    return this;
  }

  public VertxInfluxDbOptions getInfluxDbOptions() {
    return influxDbOptions;
  }

  /**
   * Set InfluxDB options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param influxDbOptions backend options for InfluxDB
   */
  public VertxMonitoringOptions setInfluxDbOptions(VertxInfluxDbOptions influxDbOptions) {
    this.influxDbOptions = influxDbOptions;
    return this;
  }

  public VertxPrometheusOptions getPrometheusOptions() {
    return prometheusOptions;
  }

  /**
   * Set Prometheus options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param prometheusOptions backend options for Prometheus
   */
  public VertxMonitoringOptions setPrometheusOptions(VertxPrometheusOptions prometheusOptions) {
    this.prometheusOptions = prometheusOptions;
    return this;
  }

  public VertxJmxMetricsOptions getJmxMetricsOptions() {
    return jmxMetricsOptions;
  }

  /**
   * Set JMX metrics options.
   * Setting a registry backend option is mandatory in order to effectively report metrics.
   * @param jmxMetricsOptions backend options for JMX reporting
   */
  public VertxMonitoringOptions setJmxMetricsOptions(VertxJmxMetricsOptions jmxMetricsOptions) {
    this.jmxMetricsOptions = jmxMetricsOptions;
    return this;
  }
}
