/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package io.vertx.micrometer.impl.tags;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * A wrapper for {@link Tags}.
 * <p>
 * Using this class to combine tags can be more efficient because redundant copies are avoided.
 */
public final class TagsWrapper {

  private static final TagsWrapper EMPTY = new TagsWrapper(Tags.empty());

  private final Tags tags;

  private TagsWrapper(Tags tags) {
    this.tags = tags;
  }

  public static TagsWrapper empty() {
    return EMPTY;
  }

  public static TagsWrapper of(Tag t1) {
    if (t1 == IgnoredTag.INSTANCE) {
      return empty();
    }
    return new TagsWrapper(Tags.of(t1));
  }

  public static TagsWrapper of(Tag t1, Tag t2) {
    if (t1 == IgnoredTag.INSTANCE) {
      return of(t2);
    }
    if (t2 == IgnoredTag.INSTANCE) {
      return of(t1);
    }
    return new TagsWrapper(Tags.of(t1, t2));
  }

  public TagsWrapper and(Tag t1) {
    if (t1 == IgnoredTag.INSTANCE) {
      return this;
    }
    return new TagsWrapper(tags.and(t1));
  }

  public TagsWrapper and(Tag t1, Tag t2) {
    if (t1 == IgnoredTag.INSTANCE) {
      return and(t2);
    }
    if (t2 == IgnoredTag.INSTANCE) {
      return and(t1);
    }
    return new TagsWrapper(tags.and(t1, t2));
  }

  public TagsWrapper and(Iterable<Tag> iterable) {
    if (iterable == Tags.empty()) {
      return this;
    }
    return new TagsWrapper(tags.and(iterable));
  }

  public Tags unwrap() {
    return tags;
  }
}
