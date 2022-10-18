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

package io.vertx.micrometer.impl.meters;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.meters.TagsCache.TagsData;
import org.junit.Test;

import static io.vertx.micrometer.Label.*;
import static org.junit.Assert.*;

public class TagsDataTest {

  @Test
  public void testAutoCloseable() {
    TagsData value = new TagsData(Tags.of(Tag.of("john", "doe")), new Label[]{LOCAL}, new String[]{"localhost"});
    value.close();
    assertNull(value.customTags);
    assertNull(value.keys);
    assertNull(value.values);
  }

  @Test
  public void copy() {
    Tags customTags = Tags.of(Tag.of("john", "doe"));
    Label[] keys = {REMOTE};
    String[] values = {"webserver.mycorp.int"};

    TagsData original = new TagsData(customTags, keys, values);
    TagsData copy = original.copy();

    assertEquals(original.hashCode(), copy.hashCode());
    assertEquals(original, copy);

    assertNotSame(original.customTags, copy.customTags);
    assertNotSame(original.customTags.iterator().next(), copy.customTags.iterator().next());
  }

  @Test
  public void copyNoCustomTags() {
    Label[] keys = {HTTP_METHOD};
    String[] values = {"GET"};

    TagsData original = new TagsData(null, keys, values);
    TagsData copy = original.copy();

    equals(original, copy);

    assertNull(copy.customTags);
  }

  @Test
  public void testNotEquals() {
    Tag john = Tag.of("john", "doe");
    Tag jane = Tag.of("jane", "doe");
    Tag durant = Tag.of("john", "durant");

    notEquals(
      new TagsData(Tags.of(jane, john), new Label[]{LOCAL}, new String[]{"localhost"}),
      new TagsData(Tags.of(jane, durant), new Label[]{LOCAL}, new String[]{"localhost"})
    );

    notEquals(
      new TagsData(Tags.of(john, jane), new Label[]{LOCAL}, new String[]{"localhost"}),
      new TagsData(Tags.of(jane, jane), new Label[]{LOCAL}, new String[]{"localhost"})
    );

    notEquals(
      new TagsData(Tags.of(jane), new Label[]{LOCAL}, new String[]{"localhost"}),
      new TagsData(Tags.of(jane, john), new Label[]{LOCAL}, new String[]{"localhost"})
    );

    notEquals(
      new TagsData(null, new Label[]{LOCAL}, new String[]{"localhost"}),
      new TagsData(Tags.of(jane), new Label[]{LOCAL}, new String[]{"localhost"})
    );

    notEquals(
      new TagsData(Tags.of(john), new Label[]{LOCAL}, new String[]{"localhost"}),
      new TagsData(null, new Label[]{LOCAL}, new String[]{"localhost"})
    );

  }

  private void equals(TagsData t1, TagsData t2) {
    assertEquals(t1, t2);
    assertEquals(t1.hashCode(), t2.hashCode());
  }

  private void notEquals(TagsData t1, TagsData t2) {
    assertNotEquals(t1, t2);
    assertNotEquals(t1.hashCode(), t2.hashCode());
  }
}
