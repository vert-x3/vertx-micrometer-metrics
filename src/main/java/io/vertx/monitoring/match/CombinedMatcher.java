package io.vertx.monitoring.match;

/**
 * @author Joel Takvorian
 */
class CombinedMatcher implements Matcher {

  private final Matcher matcher1;
  private final Matcher matcher2;

  CombinedMatcher(Matcher m1, Matcher m2) {
    this.matcher1 = m1;
    this.matcher2 = m2;
  }

  @Override
  public String matches(String value) {
    String match = matcher1.matches(value);
    if (match != null) {
      return match;
    }
    return matcher2.matches(value);
  }
}
