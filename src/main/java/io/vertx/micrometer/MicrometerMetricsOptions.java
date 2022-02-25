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
import io.micrometer.core.instrument.Tag;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.observability.HttpRequest;

import java.util.*;
import java.util.function.Function;

/**
 * Vert.x micrometer configuration.
 * <p>
 * It is required to set either {@code influxDbOptions}, {@code prometheusOptions} or {@code jmxMetricsOptions}
 * (or, programmatically, {@code micrometerRegistry}) in order to actually report metrics.
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
  public static final List<Label> DEFAULT_LABELS = Arrays.asList(Label.HTTP_ROUTE, Label.HTTP_METHOD, Label.HTTP_CODE, Label.POOL_TYPE, Label.EB_SIDE);

  /**
   * Whether JVM metrics should be collected by default = false.
   */
  public static final boolean DEFAULT_JVM_METRICS_ENABLED = false;

  /**
   * Default metrics naming = Vert.x 4 naming
   */
  public static final MetricsNaming DEFAULT_METRICS_NAMING = MetricsNaming.v4Names();

  private Set<String> disabledMetricsCategories;
  private String registryName;
  private Set<Label> labels;
  private List<Match> labelMatches;
  private MeterRegistry micrometerRegistry;
  private VertxInfluxDbOptions influxDbOptions;
  private VertxPrometheusOptions prometheusOptions;
  private VertxJmxMetricsOptions jmxMetricsOptions;
  private boolean jvmMetricsEnabled;
  private MetricsNaming metricsNaming;
  private Function<HttpRequest, Iterable<Tag>> serverRequestTagsProvider;
  private Function<HttpRequest, Iterable<Tag>> clientRequestTagsProvider;

  /**
   * Creates default options for Micrometer metrics.
   */
  public MicrometerMetricsOptions() {
    disabledMetricsCategories = new HashSet<>();
    registryName = DEFAULT_REGISTRY_NAME;
    labels = EnumSet.copyOf(DEFAULT_LABELS);
    labelMatches = new ArrayList<>();
    jvmMetricsEnabled = DEFAULT_JVM_METRICS_ENABLED;
    metricsNaming = DEFAULT_METRICS_NAMING;
    serverRequestTagsProvider = null;
    clientRequestTagsProvider = null;
  }

  /**
   * Creates new options object for Micrometer metrics, which is a copy of {@code other}.
   */
  public MicrometerMetricsOptions(MicrometerMetricsOptions other) {
    super(other);
    disabledMetricsCategories = other.disabledMetricsCategories != null ? new HashSet<>(other.disabledMetricsCategories) : new HashSet<>();
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
    jvmMetricsEnabled = other.jvmMetricsEnabled;
    metricsNaming = other.metricsNaming;
    serverRequestTagsProvider = other.serverRequestTagsProvider;
    clientRequestTagsProvider = other.clientRequestTagsProvider;
  }

  /**
   * Creates new options object for Micrometer metrics from {@code json} input.
   */
  public MicrometerMetricsOptions(JsonObject json) {
    this();
    MicrometerMetricsOptionsConverter.fromJson(json, this);
    labelMatches = loadLabelMatches(json);
  }

  /**
   * @return a JSON representation of these options
   */
  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    MicrometerMetricsOptionsConverter.toJson(this, json);
    return json;
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

  @Override
  public MicrometerMetricsOptions setFactory(VertxMetricsFactory factory) {
    super.setFactory(factory);
    return this;
  }

  /**
   * @return the disabled metrics types.
   */
  public Set<String> getDisabledMetricsCategories() {
    return disabledMetricsCategories;
  }

  /**
   * Sets metrics types that are disabled.
   *
   * @param disabledMetricsCategories to specify the set of metrics types to be disabled.
   * @return a reference to this, so that the API can be used fluently
   */
  public MicrometerMetricsOptions setDisabledMetricsCategories(Set<String> disabledMetricsCategories) {
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
      disabledMetricsCategories = new HashSet<>();
    }
    this.disabledMetricsCategories.add(metricsDomain.toCategory());
    return this;
  }

  /**
   * Set metric that will not be registered. Schedulers will check the set {@code disabledMetricsCategories} when
   * registering metrics suppliers
   *
   * @param category the type of metrics
   * @return a reference to this, so that the API can be used fluently
   */
  @GenIgnore
  public MicrometerMetricsOptions addDisabledMetricsCategory(String category) {
    if (disabledMetricsCategories == null) {
      disabledMetricsCategories = new HashSet<>();
    }
    this.disabledMetricsCategories.add(category);
    return this;
  }

  /**
   * Is the given metrics category disabled?
   * @return true if it is disabled
   */
  @GenIgnore
  public boolean isMetricsCategoryDisabled(MetricsDomain metricsDomain) {
    return disabledMetricsCategories != null && disabledMetricsCategories.contains(metricsDomain.toCategory());
  }

  /**
   * Is the given metrics category disabled?
   * @return true if it is disabled
   */
  @GenIgnore
  public boolean isMetricsCategoryDisabled(String category) {
    return disabledMetricsCategories != null && disabledMetricsCategories.contains(category);
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

  /**
   * @return true if JVM metrics should be collected, false otherwise
   */
  public boolean isJvmMetricsEnabled() {
    return jvmMetricsEnabled;
  }

  /**
   * Whether JVM metrics should be collected. Defaults to {@code false}.
   *
   * @param jvmMetricsEnabled true to collect JVM metrics, false otherwise. Defaults to {@code false}.
   * @return a reference to this, so the API can be used fluently
   */
  public MicrometerMetricsOptions setJvmMetricsEnabled(boolean jvmMetricsEnabled) {
    this.jvmMetricsEnabled = jvmMetricsEnabled;
    return this;
  }

  /**
   * {@code MetricsNaming} is a structure that holds names of all metrics, each one can be changed individually.
   * @return the configured {@code MetricsNaming} object (defaults to Vert.x names).
   */
  public MetricsNaming getMetricsNaming() {
    return metricsNaming;
  }

  /**
   * {@code MetricsNaming} is a structure that holds names of all metrics, each one can be changed individually.
   * For instance, to retrieve compatibility with the names used in Vert.x 3.x, use {@code setMetricsNaming(MetricsNaming.v3Names())}
   *
   * @param metricsNaming a {@code MetricsNaming} object.
   * @return a reference to this, so the API can be used fluently
   */
  public MicrometerMetricsOptions setMetricsNaming(MetricsNaming metricsNaming) {
    this.metricsNaming = metricsNaming;
    return this;
  }

  /**
   * @return an optional custom tags provider for HTTP server requests
   * @deprecated use {@code getServerRequestTagsProvider} instead
   */
  @GenIgnore
  @Deprecated
  public Function<HttpRequest, Iterable<Tag>> getRequestsTagsProvider() {
    return this.getServerRequestTagsProvider();
  }

  /**
   * Sets a custom tags provider for HTTP server requests. Allows to generate custom tags for every {@code HttpRequest} object processed through the metrics SPI.
   *
   * @param serverRequestTagsProvider an object implementing the {@code CustomTagsProvider} interface for {@code HttpRequest}.
   * @return a reference to this, so that the API can be used fluently
   * @deprecated use {@code setServerRequestTagsProvider} instead
   */
  @GenIgnore
  @Deprecated
  public MicrometerMetricsOptions setRequestsTagsProvider(Function<HttpRequest, Iterable<Tag>> serverRequestTagsProvider) {
    return this.setServerRequestTagsProvider(serverRequestTagsProvider);
  }

  /**
   * @return an optional custom tags provider for HTTP server requests
   */
  @GenIgnore
  public Function<HttpRequest, Iterable<Tag>> getServerRequestTagsProvider() {
    return serverRequestTagsProvider;
  }

  /**
   * Sets a custom tags provider for HTTP server requests. Allows to generate custom tags for every {@code HttpRequest} object processed through the metrics SPI.
   *
   * @param serverRequestTagsProvider an object that returns an iterable of {@code Tag} for a {@code HttpRequest}.
   * @return a reference to this, so that the API can be used fluently
   */
  @GenIgnore
  public MicrometerMetricsOptions setServerRequestTagsProvider(Function<HttpRequest, Iterable<Tag>> serverRequestTagsProvider) {
    this.serverRequestTagsProvider = serverRequestTagsProvider;
    return this;
  }

  /**
   * @return an optional custom tags provider for HTTP client requests
   */
  @GenIgnore
  public Function<HttpRequest, Iterable<Tag>> getClientRequestTagsProvider() {
    return clientRequestTagsProvider;
  }

  /**
   * Sets a custom tags provider for HTTP client requests. Allows to generate custom tags for every {@code HttpRequest} object processed through the metrics SPI.
   *
   * @param clientRequestTagsProvider an object that returns an iterable of {@code Tag} for a {@code HttpRequest}.
   * @return a reference to this, so that the API can be used fluently
   */
  @GenIgnore
  public MicrometerMetricsOptions setClientRequestTagsProvider(Function<HttpRequest, Iterable<Tag>> clientRequestTagsProvider) {
    this.clientRequestTagsProvider = clientRequestTagsProvider;
    return this;
  }
}
