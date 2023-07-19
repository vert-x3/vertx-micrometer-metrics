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

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.impl.ContextInternal;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.impl.Labels;

import java.util.*;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Cache {@link Tags} in Vert.x EL contexts.
 * Each Vert.x EL context gets its own cache to avoid concurrency complications.
 * In case this cache is called on a non-Vert.x thread or a worker thread, a new instance of {@link Tags} is returned.
 */
public class TagsCache {

  // To avoid creating cache keys on each lookup, a flyweight is stored in the Vert.x context
  // It is possible to have this flyweight because the caller is on an event loop thread
  private static final Object TAGS_DATA_FLYWEIGHT = new Object();
  // Used to store the per-context Tags cache
  private static final Object CACHE = new Object();

  public static Tags getOrCreate(Iterable<Tag> customTags, Label[] keys, String[] values) {
    ContextInternal context = ContextInternal.current();
    if (context == null || context.isWorkerContext() || !context.inThread()) {
      return createTags(customTags, keys, values);
    }
    Map<TagsData, Tags> cache = cache(context);
    try (TagsData cacheKey = flyweightKey(context, customTags, keys, values)) {
      Tags tags = cache.get(cacheKey);
      if (tags == null) {
        tags = createTags(customTags, keys, values);
        cache.put(cacheKey.copy(), tags);
      }
      return tags;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<TagsData, Tags> cache(ContextInternal context) {
    return (Map<TagsData, Tags>) context.contextData().computeIfAbsent(CACHE, v -> lruMap());
  }

  private static TagsData flyweightKey(ContextInternal context, Iterable<Tag> customTags, Label[] keys, String[] values) {
    TagsData tagsData = (TagsData) context.contextData().computeIfAbsent(TAGS_DATA_FLYWEIGHT, v -> new TagsData());
    tagsData.init(customTags, keys, values);
    return tagsData;
  }

  // Visible for testing
  static Tags createTags(Iterable<Tag> customTags, Label[] keys, String[] values) {
    return Labels.toTags(keys, values).and(customTags);
  }

  private static LinkedHashMap<TagsData, Tags> lruMap() {
    return new LinkedHashMap<TagsData, Tags>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > 512;
      }
    };
  }

  // Visible for testing
  final static class TagsData implements AutoCloseable {
    Iterable<Tag> customTags;
    Label[] keys;
    String[] values;

    TagsData() {
    }

    TagsData(Iterable<Tag> customTags, Label[] keys, String[] values) {
      init(customTags, keys, values);
    }

    void init(Iterable<Tag> customTags, Label[] keys, String[] values) {
      this.customTags = customTags;
      this.keys = keys;
      this.values = values;
    }

    TagsData copy() {
      if (customTags == null) {
        return new TagsData(null, keys, values);
      }
      // Copy the custom tags, the user-provided iterable may retain objects that should be collected later
      ArrayList<Tag> tags = new ArrayList<>();
      for (Tag tag : customTags) {
        // The only Tag implementation provided by Micrometer is immutable
        // but users may provide their own
        tags.add(new ImmutableTag(tag.getKey(), tag.getValue()));
      }
      tags.trimToSize();
      // No need to copy keys and values, they come from our implementation
      return new TagsData(tags, keys, values);
    }

    @Override
    public void close() {
      init(null, null, null);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TagsData other = (TagsData) o;
      if (customTags == null) {
        if (other.customTags != null) {
          return false;
        }
      } else if (other.customTags == null) {
        return false;
      } else {
        Iterator<Tag> iter = customTags.iterator();
        Iterator<Tag> otherIter = other.customTags.iterator();
        while (iter.hasNext()) {
          if (!otherIter.hasNext()) {
            return false;
          }
          Tag tag1 = iter.next();
          Tag tag2 = otherIter.next();
          if (!tag1.getKey().equals(tag2.getKey()) || !tag1.getValue().equals(tag2.getValue())) {
            return false;
          }
        }
        if (otherIter.hasNext()) {
          return false;
        }
      }
      return Arrays.equals(keys, other.keys) && Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
      int result;
      if (customTags == null) {
        result = 0;
      } else {
        result = 1;
        for (Tag tag : customTags) {
          result = 31 * result + tag.getKey().hashCode();
          result = 31 * result + tag.getValue().hashCode();
        }
      }
      result = 31 * result + Arrays.hashCode(keys);
      result = 31 * result + Arrays.hashCode(values);
      return result;
    }

    @Override
    public String toString() {
      return "TagsData{" +
        "customTags=" + (customTags == null ? null : StreamSupport.stream(customTags.spliterator(), false).collect(toList())) +
        ", keys=" + Arrays.toString(keys) +
        ", values=" + Arrays.toString(values) +
        '}';
    }
  }

  private TagsCache() {
    // Utility
  }
}
