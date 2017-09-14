package org.infalible.selenium.remote;

import java.util.Comparator;
import java.util.function.Supplier;

class DefaultProviderComparator implements Comparator<Supplier<ActiveSession>> {
  @Override
  public int compare(Supplier<ActiveSession> lhs, Supplier<ActiveSession> rhs) {
    if (lhs.equals(rhs)) {
      return 0;
    }

    // If both sides are themselves Comparable, use that.
    if (lhs instanceof Comparable && rhs instanceof Comparable) {
      @SuppressWarnings("unchecked")
      Comparable<Supplier<ActiveSession>> comparable = (Comparable<Supplier<ActiveSession>>) lhs;
      return comparable.compareTo(rhs);
    }

    // If one side is Comparable, prefer that
    if (lhs instanceof Comparable) {
      return 1;
    }

    if (rhs instanceof Comparable) {
      return -1;
    }

    // Otherwise, we don't care
    return 0;
  }
}
