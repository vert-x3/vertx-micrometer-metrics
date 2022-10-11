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

package io.vertx.micrometer.impl;

import io.micrometer.core.instrument.Tags;
import io.vertx.micrometer.Label;
import org.junit.Test;

import static io.vertx.micrometer.Label.*;
import static org.junit.Assert.*;

public class LabelsTest {

  @Test
  public void toTags() {
    Tags expected, actual;

    expected = Tags.empty();
    actual = Labels.toTags(new Label[]{}, new String[]{null, "/eventbus/*"});
    assertEquals(expected, actual);

    expected = Tags.of(HTTP_CODE.toString(), "200");
    actual = Labels.toTags(new Label[]{HTTP_CODE}, new String[]{"200"});
    assertEquals(expected, actual);

    expected = Tags.of(HTTP_CODE.toString(), "200", HTTP_ROUTE.toString(), "/eventbus/*");
    actual = Labels.toTags(new Label[]{HTTP_CODE, HTTP_ROUTE}, new String[]{"200", "/eventbus/*"});
    assertEquals(expected, actual);

    expected = Tags.of(HTTP_ROUTE.toString(), "/eventbus/*");
    actual = Labels.toTags(new Label[]{HTTP_CODE, HTTP_ROUTE}, new String[]{null, "/eventbus/*"});
    assertEquals(expected, actual);

    expected = Tags.empty();
    actual = Labels.toTags(new Label[]{HTTP_CODE, HTTP_ROUTE}, new String[]{null, null});
    assertEquals(expected, actual);
  }
}
