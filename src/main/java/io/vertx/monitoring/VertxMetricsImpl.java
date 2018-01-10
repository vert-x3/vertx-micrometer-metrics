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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Verticle;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.monitoring.backend.BackendRegistries;
import io.vertx.monitoring.backend.BackendRegistry;
import io.vertx.monitoring.match.LabelMatchers;

import java.util.Optional;

import static io.vertx.monitoring.MetricsCategory.*;

/**
 * Metrics SPI implementation for Micrometer.
 *
 * @author Joel Takvorian
 */
public class VertxMetricsImpl extends AbstractMetrics implements VertxMetrics {
  private final BackendRegistry backendRegistry;
  private final String registryName;
  private final Optional<EventBusMetrics> eventBusMetrics;
  private final Optional<DatagramSocketMetrics> datagramSocketMetrics;
  private final Optional<VertxNetClientMetrics> netClientMetrics;
  private final Optional<VertxNetServerMetrics> netServerMetrics;
  private final Optional<VertxHttpClientMetrics> httpClientMetrics;
  private final Optional<VertxHttpServerMetrics> httpServerMetrics;
  private final Optional<VertxPoolMetrics> poolMetrics;
  private final Optional<VertxVerticleMetrics> verticleMetrics;

  /**
   * @param options Vertx Prometheus options
   */
  public VertxMetricsImpl(VertxMonitoringOptions options, LabelMatchers labelMatchers, BackendRegistry backendRegistry) {
    super(labelMatchers, backendRegistry.getMeterRegistry(), null, null);
    this.backendRegistry = backendRegistry;
    registryName = options.getRegistryName();
    MeterRegistry registry = backendRegistry.getMeterRegistry();

    eventBusMetrics = options.isMetricsCategoryDisabled(EVENT_BUS) ? Optional.empty()
      : Optional.of(new VertxEventBusMetrics(labelMatchers, registry));
    datagramSocketMetrics = options.isMetricsCategoryDisabled(DATAGRAM_SOCKET) ? Optional.empty()
      : Optional.of(new VertxDatagramSocketMetrics(labelMatchers, registry));
    netClientMetrics = options.isMetricsCategoryDisabled(NET_CLIENT) ? Optional.empty()
      : Optional.of(new VertxNetClientMetrics(labelMatchers, registry));
    netServerMetrics = options.isMetricsCategoryDisabled(NET_SERVER) ? Optional.empty()
      : Optional.of(new VertxNetServerMetrics(labelMatchers, registry));
    httpClientMetrics = options.isMetricsCategoryDisabled(HTTP_CLIENT) ? Optional.empty()
      : Optional.of(new VertxHttpClientMetrics(labelMatchers, registry));
    httpServerMetrics = options.isMetricsCategoryDisabled(HTTP_SERVER) ? Optional.empty()
      : Optional.of(new VertxHttpServerMetrics(labelMatchers, registry));
    poolMetrics = options.isMetricsCategoryDisabled(NAMED_POOLS) ? Optional.empty()
      : Optional.of(new VertxPoolMetrics(labelMatchers, registry));
    verticleMetrics = options.isMetricsCategoryDisabled(VERTICLES) ? Optional.empty()
      : Optional.of(new VertxVerticleMetrics(labelMatchers, registry));
  }

  @Override
  public void eventBusInitialized(EventBus bus) {
    backendRegistry.eventBusInitialized(bus);
  }

  @Override
  public void verticleDeployed(Verticle verticle) {
    verticleMetrics.ifPresent(vm -> vm.verticleDeployed(verticle));
  }

  @Override
  public void verticleUndeployed(Verticle verticle) {
    verticleMetrics.ifPresent(vm -> vm.verticleUndeployed(verticle));
  }

  @Override
  public void timerCreated(long l) {
  }

  @Override
  public void timerEnded(long l, boolean b) {
  }

  @Override
  public EventBusMetrics createMetrics(EventBus eventBus) {
    return eventBusMetrics.orElse(DummyVertxMetrics.DummyEventBusMetrics.INSTANCE);
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createMetrics(HttpServer httpServer, SocketAddress socketAddress, HttpServerOptions httpServerOptions) {
    return httpServerMetrics
      .map(servers -> servers.forAddress(socketAddress))
      .orElse(DummyVertxMetrics.DummyHttpServerMetrics.INSTANCE);
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?, ?> createMetrics(HttpClient httpClient, HttpClientOptions httpClientOptions) {
    return httpClientMetrics
      .map(clients -> clients.forAddress(httpClientOptions.getLocalAddress()))
      .orElse(DummyVertxMetrics.DummyHttpClientMetrics.INSTANCE);
  }

  @Override
  public TCPMetrics<?> createMetrics(SocketAddress socketAddress, NetServerOptions netServerOptions) {
    return netServerMetrics
      .map(servers -> servers.forAddress(socketAddress))
      .orElse(DummyVertxMetrics.DummyTCPMetrics.INSTANCE);
  }

  @Override
  public TCPMetrics<?> createMetrics(NetClientOptions netClientOptions) {
    return netClientMetrics
      .map(clients -> clients.forAddress(netClientOptions.getLocalAddress()))
      .orElse(DummyVertxMetrics.DummyTCPMetrics.INSTANCE);
  }

  @Override
  public DatagramSocketMetrics createMetrics(DatagramSocket datagramSocket, DatagramSocketOptions datagramSocketOptions) {
    return datagramSocketMetrics.orElse(DummyVertxMetrics.DummyDatagramMetrics.INSTANCE);
  }

  @Override
  public <P> PoolMetrics<?> createMetrics(P pool, String poolType, String poolName, int maxPoolSize) {
    return poolMetrics
      .map(pools -> pools.forInstance(poolType, poolName, maxPoolSize))
      .orElse(DummyVertxMetrics.DummyWorkerPoolMetrics.INSTANCE);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void close() {
    BackendRegistries.stop(registryName);
  }
}
