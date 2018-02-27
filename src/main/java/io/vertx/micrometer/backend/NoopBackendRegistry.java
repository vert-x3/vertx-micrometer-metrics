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
package io.vertx.micrometer.backend;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.vertx.core.eventbus.EventBus;

/**
 * @author Joel Takvorian
 */
public enum NoopBackendRegistry implements BackendRegistry {
  INSTANCE;

  @Override
  public MeterRegistry getMeterRegistry() {
    return Metrics.globalRegistry;
  }

  @Override
  public void eventBusInitialized(EventBus bus) {
  }

  @Override
  public void close() {
  }
}
