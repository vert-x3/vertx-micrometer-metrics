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
package io.vertx.ext.monitoring.collector;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.monitoring.collector.impl.BatchingVertxMetrics;
import io.vertx.ext.monitoring.collector.impl.DataPoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Joel Takvorian
 */
public class DummyVertxMetrics extends BatchingVertxMetrics<BatchingReporterOptions> {

  public static final DummyReporter REPORTER = new DummyReporter();

  public DummyVertxMetrics(Vertx vertx, BatchingReporterOptions options) {
    super(vertx, options);
  }

  @Override
  public Reporter createReporter(Context context) {
    return REPORTER;
  }

  public static class DummyReporter implements Reporter {
    private final List<Watcher> watchers = new CopyOnWriteArrayList<>();

    @Override
    public void stop() {
    }

    @Override
    public void handle(List<DataPoint> dataPoints) {
      watchers.forEach(watcher -> {
        if (dataPoints.stream().anyMatch(watcher.waitUntil)) {
          List<DataPoint> filtered = dataPoints.stream().filter(dp -> watcher.filter.test(dp.getName())).collect(Collectors.toList());
          if (!filtered.isEmpty()) {
            watcher.handler.accept(filtered);
          }
        }
      });
    }

    public Object watch(Predicate<String> filter, Predicate<DataPoint> waitUntil, Consumer<List<DataPoint>> handler) {
      Watcher watcher = new Watcher(filter, waitUntil, handler);
      watchers.add(watcher);
      return watcher;
    }

    public void remove(Object ref) {
      watchers.remove((Watcher)ref);
    }

    private static class Watcher {
      private Predicate<String> filter;
      private Predicate<DataPoint> waitUntil;
      private Consumer<List<DataPoint>> handler;
      Watcher(Predicate<String> filter, Predicate<DataPoint> waitUntil, Consumer<List<DataPoint>> handler) {
        this.filter = filter;
        this.waitUntil = waitUntil;
        this.handler = handler;
      }
    }
  }
}
