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

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.BackendRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.vertx.micrometer.MetricsDomain.DATAGRAM_SOCKET;
import static io.vertx.micrometer.MetricsDomain.EVENT_BUS;
import static io.vertx.micrometer.MetricsDomain.HTTP_CLIENT;
import static io.vertx.micrometer.MetricsDomain.HTTP_SERVER;
import static io.vertx.micrometer.MetricsDomain.NAMED_POOLS;
import static io.vertx.micrometer.MetricsDomain.NET_CLIENT;
import static io.vertx.micrometer.MetricsDomain.NET_SERVER;

/**
 * Metrics SPI implementation for Micrometer.
 *
 * @author Joel Takvorian
 */
public class VertxMetricsImpl extends AbstractMetrics implements VertxMetrics {
  private final BackendRegistry backendRegistry;
  private final String registryName;
  private final MetricsNaming names;
  private final EventBusMetrics eventBusMetrics;
  private final DatagramSocketMetrics datagramSocketMetrics;
  private final VertxNetClientMetrics netClientMetrics;
  private final VertxNetServerMetrics netServerMetrics;
  private final VertxHttpClientMetrics httpClientMetrics;
  private final VertxHttpServerMetrics httpServerMetrics;
  private final VertxPoolMetrics poolMetrics;
  private final Map<String, VertxClientMetrics> mapClientMetrics = new ConcurrentHashMap<>();
  private final Set<String> disabledCategories = new HashSet<>();

  /**
   * @param options Vertx Prometheus options
   */
  public VertxMetricsImpl(MicrometerMetricsOptions options, BackendRegistry backendRegistry) {
    super(backendRegistry.getMeterRegistry());
    this.backendRegistry = backendRegistry;
    registryName = options.getRegistryName();
    MeterRegistry registry = backendRegistry.getMeterRegistry();
    if (options.getDisabledMetricsCategories() != null) {
      disabledCategories.addAll(options.getDisabledMetricsCategories());
    }
    names = options.getMetricsNaming();

    eventBusMetrics = options.isMetricsCategoryDisabled(EVENT_BUS) ? null
      : new VertxEventBusMetrics(registry, names);
    datagramSocketMetrics = options.isMetricsCategoryDisabled(DATAGRAM_SOCKET) ? null
      : new VertxDatagramSocketMetrics(registry, names);
    netClientMetrics = options.isMetricsCategoryDisabled(NET_CLIENT) ? null
      : new VertxNetClientMetrics(registry, names);
    netServerMetrics = options.isMetricsCategoryDisabled(NET_SERVER) ? null
      : new VertxNetServerMetrics(registry, names);
    httpClientMetrics = options.isMetricsCategoryDisabled(HTTP_CLIENT) ? null
      : new VertxHttpClientMetrics(registry, names, options.getClientRequestTagsProvider());
    httpServerMetrics = options.isMetricsCategoryDisabled(HTTP_SERVER) ? null
      : new VertxHttpServerMetrics(registry, names, options.getServerRequestTagsProvider());
    poolMetrics = options.isMetricsCategoryDisabled(NAMED_POOLS) ? null
      : new VertxPoolMetrics(registry, names);
  }

  void init() {
    backendRegistry.init();
  }

  @Override
  public EventBusMetrics createEventBusMetrics() {
    if (eventBusMetrics != null) {
      return eventBusMetrics;
    }
    return DummyVertxMetrics.DummyEventBusMetrics.INSTANCE;
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(HttpServerOptions httpClientOptions, SocketAddress socketAddress) {
    if (httpServerMetrics != null) {
      return httpServerMetrics.forAddress(socketAddress);
    }
    return DummyVertxMetrics.DummyHttpServerMetrics.INSTANCE;
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?> createHttpClientMetrics(HttpClientOptions httpClientOptions) {
    if (httpClientMetrics != null) {
      return httpClientMetrics.forAddress(httpClientOptions.getLocalAddress());
    }
    return DummyVertxMetrics.DummyHttpClientMetrics.INSTANCE;
  }

  @Override
  public TCPMetrics<?> createNetServerMetrics(NetServerOptions netServerOptions, SocketAddress socketAddress) {
    if (netServerMetrics != null) {
      return netServerMetrics.forAddress(socketAddress);
    }
    return DummyVertxMetrics.DummyTCPMetrics.INSTANCE;
  }

  @Override
  public TCPMetrics<?> createNetClientMetrics(NetClientOptions netClientOptions) {
    if (netClientMetrics != null) {
      return netClientMetrics.forAddress(netClientOptions.getLocalAddress());
    }
    return DummyVertxMetrics.DummyTCPMetrics.INSTANCE;
  }

  @Override
  public DatagramSocketMetrics createDatagramSocketMetrics(DatagramSocketOptions options) {
    if (datagramSocketMetrics != null) {
      return datagramSocketMetrics;
    }
    return DummyVertxMetrics.DummyDatagramMetrics.INSTANCE;
  }

  @Override
  public PoolMetrics<?> createPoolMetrics(String poolType, String poolName, int maxPoolSize) {
    if (poolMetrics != null) {
      return poolMetrics.forInstance(poolType, poolName, maxPoolSize);
    }
    return DummyVertxMetrics.DummyWorkerPoolMetrics.INSTANCE;
  }

  @Override
  public ClientMetrics<?, ?, ?, ?> createClientMetrics(SocketAddress remoteAddress, String type, String namespace) {
    if (disabledCategories.contains(type)) {
      return DummyVertxMetrics.DummyClientMetrics.INSTANCE;
    }
    VertxClientMetrics clientMetrics = mapClientMetrics.computeIfAbsent(type, t -> new VertxClientMetrics(registry, type, names));
    return clientMetrics.forInstance(remoteAddress, namespace);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Override
  public void close() {
    BackendRegistries.stop(registryName);
  }
}
