/*
 * Copyright (c) 2011-2023 The original author or authors
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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.*;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.backends.BackendRegistry;
import io.vertx.micrometer.impl.meters.LongGauges;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static io.vertx.core.metrics.impl.DummyVertxMetrics.*;
import static io.vertx.micrometer.MetricsDomain.*;

/**
 * Metrics SPI implementation for Micrometer.
 *
 * @author Joel Takvorian
 */
public class VertxMetricsImpl extends AbstractMetrics implements VertxMetrics {
  private final BackendRegistry backendRegistry;
  private final String registryName;
  private final Set<String> disabledCategories;
  private final boolean bindJvmMetrics;
  private final boolean bindNettyMetrics;
  private final List<MeterBinder> meterBinders;
  private final Function<HttpRequest, Iterable<Tag>> serverRequestTagsProvider;
  private final Function<HttpRequest, Iterable<Tag>> clientRequestTagsProvider;

  public VertxMetricsImpl(MicrometerMetricsOptions options, BackendRegistry backendRegistry, LongGauges longGauges, MeterCache meterCache) {
    super(backendRegistry.getMeterRegistry(), options.getMetricsNaming(), longGauges, EnumSet.copyOf(options.getLabels()), meterCache);
    this.backendRegistry = backendRegistry;
    registryName = options.getRegistryName();
    if (options.getDisabledMetricsCategories() != null) {
      disabledCategories = new HashSet<>(options.getDisabledMetricsCategories());
    } else {
      disabledCategories = Collections.emptySet();
    }
    bindJvmMetrics = options.isJvmMetricsEnabled();
    bindNettyMetrics = options.isNettyMetricsEnabled();
    meterBinders = new CopyOnWriteArrayList<>();
    serverRequestTagsProvider = options.getServerRequestTagsProvider();
    clientRequestTagsProvider = options.getClientRequestTagsProvider();
  }

  void init() {
    backendRegistry.init();
    if (bindJvmMetrics) {
      addMeterBinder(new ClassLoaderMetrics());
      addMeterBinder(new JvmCompilationMetrics());
      addMeterBinder(new JvmGcMetrics());
      addMeterBinder(new JvmHeapPressureMetrics());
      addMeterBinder(new JvmInfoMetrics());
      addMeterBinder(new JvmMemoryMetrics());
      addMeterBinder(new JvmThreadMetrics());
      addMeterBinder(new ProcessorMetrics());
      addMeterBinder(new UptimeMetrics());
    }
    if (bindNettyMetrics) {
      if (VertxByteBufAllocator.UNPOOLED_ALLOCATOR instanceof ByteBufAllocatorMetricProvider) {
        addMeterBinder(new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.UNPOOLED_ALLOCATOR));
      }
      if (VertxByteBufAllocator.POOLED_ALLOCATOR instanceof ByteBufAllocatorMetricProvider) {
        addMeterBinder(new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.POOLED_ALLOCATOR));
      }
    }
  }

  private void addMeterBinder(MeterBinder meterBinder) {
    meterBinders.add(meterBinder);
    meterBinder.bindTo(registry);
  }

  @Override
  public void vertxCreated(Vertx vertx) {
    if (bindNettyMetrics) {
      VertxInternal vi = (VertxInternal) vertx;
      addMeterBinder(new NettyEventExecutorMetrics(vi.getAcceptorEventLoopGroup()));
      addMeterBinder(new NettyEventExecutorMetrics(vi.getEventLoopGroup()));
    }
  }

  @Override
  public EventBusMetrics<?> createEventBusMetrics() {
    if (disabledCategories.contains(EVENT_BUS.toCategory())) {
      return DummyEventBusMetrics.INSTANCE;
    }
    return new VertxEventBusMetrics(this);
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(HttpServerOptions httpClientOptions, SocketAddress socketAddress) {
    if (disabledCategories.contains(HTTP_SERVER.toCategory())) {
      return DummyHttpServerMetrics.INSTANCE;
    }
    return new VertxHttpServerMetrics(this, serverRequestTagsProvider, socketAddress);
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?> createHttpClientMetrics(HttpClientOptions httpClientOptions) {
    if (disabledCategories.contains(HTTP_CLIENT.toCategory())) {
      return DummyHttpClientMetrics.INSTANCE;
    }
    return new VertxHttpClientMetrics(this, clientRequestTagsProvider, httpClientOptions.getLocalAddress());
  }

  @Override
  public TCPMetrics<?> createNetServerMetrics(NetServerOptions netServerOptions, SocketAddress socketAddress) {
    if (disabledCategories.contains(NET_SERVER.toCategory())) {
      return DummyTCPMetrics.INSTANCE;
    }
    return new VertxNetServerMetrics(this, socketAddress);
  }

  @Override
  public TCPMetrics<?> createNetClientMetrics(NetClientOptions netClientOptions) {
    if (disabledCategories.contains(NET_CLIENT.toCategory())) {
      return DummyTCPMetrics.INSTANCE;
    }
    return new VertxNetClientMetrics(this, netClientOptions.getLocalAddress());
  }

  @Override
  public DatagramSocketMetrics createDatagramSocketMetrics(DatagramSocketOptions options) {
    if (disabledCategories.contains(DATAGRAM_SOCKET.toCategory())) {
      return DummyDatagramMetrics.INSTANCE;
    }
    return new VertxDatagramSocketMetrics(this);
  }

  @Override
  public PoolMetrics<?> createPoolMetrics(String poolType, String poolName, int maxPoolSize) {
    if (disabledCategories.contains(NAMED_POOLS.toCategory())) {
      return DummyWorkerPoolMetrics.INSTANCE;
    }
    return new VertxPoolMetrics(this, poolType, poolName, maxPoolSize);
  }

  @Override
  public ClientMetrics<?, ?, ?, ?> createClientMetrics(SocketAddress remoteAddress, String type, String namespace) {
    if (disabledCategories.contains(type)) {
      return DummyClientMetrics.INSTANCE;
    }
    return new VertxClientMetrics(this, remoteAddress, type, namespace);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Override
  public void close() {
    for (MeterBinder meterBinder : meterBinders) {
      if (meterBinder instanceof AutoCloseable) {
        AutoCloseable closeable = (AutoCloseable) meterBinder;
        try {
          closeable.close();
        } catch (Exception ignored) {
        }
      }
    }
    BackendRegistries.stop(registryName);
    if (meterCache != null) {
      meterCache.close();
    }
  }
}
