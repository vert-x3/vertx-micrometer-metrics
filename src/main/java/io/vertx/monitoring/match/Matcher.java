package io.vertx.monitoring.match;

/**
 * @author Joel Takvorian
 */
interface Matcher {
  /**
   * Return a non {@code null} identifier string when the {@code value} matches otherwise returns {@code null}.
   * <p>
   * The returned identifier can be used to identify the match, it is either the original
   * value or an alias.
   *
   * @param value the value to match
   * @return the identifier or null
   */
  String matches(String value);

  default Matcher combineThen(Matcher thenMatch) {
    return new CombinedMatcher(this, thenMatch);
  }
}
