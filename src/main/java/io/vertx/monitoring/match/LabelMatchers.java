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
package io.vertx.monitoring.match;

import io.micrometer.core.instrument.Tag;
import io.vertx.monitoring.MetricsCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Joel Takvorian
 */
public class LabelMatchers {
  private final Map<String, Matcher> labelMatchersAllDomains;
  private final Map<String, Matcher> labelMatchersPerDomain;

  public LabelMatchers(List<Match> labelMatches) {
    labelMatchersAllDomains = labelMatches.stream()
      .filter(m -> m.getDomain() == null)
      .collect(Collectors.groupingBy(
        Match::getLabel,
        Collectors.collectingAndThen(Collectors.toList(), SimpleMatcher::new)));

    labelMatchersPerDomain = labelMatches.stream()
      .filter(m -> m.getDomain() != null)
      .collect(Collectors.groupingBy(
        m -> m.getDomain().toString() + "/" + m.getLabel(),
        Collectors.collectingAndThen(Collectors.toList(), SimpleMatcher::new)));
  }

  public List<Tag> toTags(MetricsCategory domain, String[] keys, String[] values) {
    if (keys.length == 0) {
      return Collections.emptyList();
    }
    List<Tag> tags = new ArrayList<>(keys.length);
    for (int i = 0; i < keys.length; i++) {
      Matcher matcher = getMatcher(domain, keys[i]);
      if (matcher != null) {
        String match = matcher.matches(values[i]);
        if (match == null) {
          // If one match fails, the measurement is rejected
          return null;
        }
        tags.add(Tag.of(keys[i], match));
      } else {
        tags.add(Tag.of(keys[i], values[i]));
      }
    }
    return tags;
  }

  private Matcher getMatcher(MetricsCategory domain, String key) {
    Matcher matcher1 = labelMatchersPerDomain.get(domain.toString() + "/" + key);
    Matcher matcher2 = labelMatchersAllDomains.get(key);
    if (matcher1 != null) {
      if (matcher2 != null) {
        return matcher1.combineThen(matcher2);
      }
      return matcher1;
    }
    return matcher2;
  }
}
