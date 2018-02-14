/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import io.micrometer.core.instrument.Tag;
import io.vertx.core.net.SocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Joel Takvorian
 */
public final class Labels {
  private Labels() {
  }

  public static String fromAddress(SocketAddress address) {
    return address == null ? "?" : (address.host() + ":" + address.port());
  }

  public static String getSide(boolean local) {
    return local ? "local" : "remote";
  }

  public static List<Tag> toTags(Label[] keys, String[] values) {
    if (keys.length == 0) {
      return Collections.emptyList();
    }
    List<Tag> tags = new ArrayList<>(keys.length);
    for (int i = 0; i < keys.length; i++) {
      String lowKey = keys[i].toString().toLowerCase();
      tags.add(Tag.of(lowKey, values[i]));
    }
    return tags;
  }

  public static class Values {
    private final String[] values;

    public Values(String... values) {
      this.values = values;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Values otherValues = (Values) o;
      return Arrays.equals(values, otherValues.values);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(values);
    }
  }
}
