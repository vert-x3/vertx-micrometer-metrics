/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EmptyCompositeMeterRegistryTest extends MicrometerMetricsTestBase {

  @Override
  protected MicrometerMetricsOptions metricOptions() {
    CompositeMeterRegistry emptyCompositeRegistry = new CompositeMeterRegistry();
    return new MicrometerMetricsOptions()
      .setRegistryName(registryName)
      .setMicrometerRegistry(emptyCompositeRegistry)
      .setEnabled(true);
  }

  @Test
  public void simplyStarts(TestContext ctx) {
    vertx = vertx(ctx);

    // If the task is executed then the gauge lookup succedeed
    vertx.executeBlocking(() -> null).onComplete(ctx.asyncAssertSuccess());
  }
}
