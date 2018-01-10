package io.vertx.monitoring.match;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class SimpleMatcher implements Matcher {
  private final Map<String, String> exactMatches;
  private final Entry<Pattern, String>[] regexMatches;

  SimpleMatcher(List<Match> matches) {
    exactMatches = matches.stream()
      .filter(match -> match.getType() == MatchType.EQUALS && match.getValue() != null)
      .collect(Collectors.toMap(Match::getValue, match -> match.getAlias() != null ? match.getAlias() : match.getValue()));

    @SuppressWarnings("unchecked")
    Entry<Pattern, String>[] entries = matches.stream()
      .filter(match -> match.getType() == MatchType.REGEX && match.getValue() != null)
      .map(match -> new SimpleEntry<>(Pattern.compile(match.getValue()), match.getAlias()))
      .toArray(Entry[]::new);
    regexMatches = entries;
  }

  @Override
  public String matches(String value) {
    if (exactMatches.size() > 0 && exactMatches.containsKey(value)) {
      String valueOrAlias = exactMatches.get(value);
      if (valueOrAlias != null) {
        return valueOrAlias;
      }
    }
    if (regexMatches.length > 0) {
      for (Entry<Pattern, String> entry : regexMatches) {
        if (entry.getKey().matcher(value).matches()) {
          String alias = entry.getValue();
          if (alias != null) {
            return alias;
          } else {
            return value;
          }
        }
      }
    }
    return null;
  }
}
