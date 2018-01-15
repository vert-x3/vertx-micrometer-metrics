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

import org.assertj.core.groups.Tuple;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * @author Joel Takvorian
 */
public final class Comparators {

  private Comparators() {
  }

  public static Comparator<Number> factorN(double n) {
    return (actual, expected) ->
      (actual.doubleValue() >= expected.doubleValue() && actual.doubleValue() <= n * expected.doubleValue())
        ? 0 : -1;
  }

  public static Comparator<Number> atLeast() {
    return (actual, expected) -> (actual.doubleValue() >= expected.doubleValue()) ? 0 : -1;
  }

  public static Comparator<Tuple> metricValueComparator(String metricName, Comparator<Number> comparator) {
    return metricValueComparators(Collections.singletonMap(metricName, comparator));
  }

  public static Comparator<Tuple> metricValueComparators(Map<String, Comparator<Number>> specialComparators) {
    return (t1, t2) -> {
      Object[] arr1 = t1.toArray();
      Object[] arr2 = t2.toArray();
      String name1 = (String) arr1[0];
      String name2 = (String) arr2[0];
      if (name1.equals(name2)) {
        Number num1 = (Number) arr1[1];
        Number num2 = (Number) arr2[1];
        if (specialComparators.containsKey(name1)) {
          return specialComparators.get(name1).compare(num1, num2);
        } else {
          return num1.equals(num2) ? 0 : -1;
        }
      }
      return -1;
    };
  }
}
