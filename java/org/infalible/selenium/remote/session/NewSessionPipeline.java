package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableList;
import org.openqa.selenium.SessionNotCreatedException;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class NewSessionPipeline {

  private final ImmutableList<CapabilityMatcher> matchers;
  private final Comparator<Supplier<ActiveSession>> comparator;

  private NewSessionPipeline(
      Comparator<Supplier<ActiveSession>> comparator,
      List<CapabilityMatcher> matchers) {
    this.comparator = comparator;
    this.matchers = ImmutableList.copyOf(matchers);
  }

  public static Builder builder() {
    return new Builder();
  }

  public ActiveSession newSession(NewSessionPayload payload) {
    return payload.stream()
        .flatMap(section -> matchers.stream()
            .map(matcher -> matcher.match(section.getCapabilities(), section.getMetadata()))
            .filter(Objects::nonNull))
        .sorted(comparator)
        .map(supplier -> {
          try {
            return supplier.get();
          } catch (Exception e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new SessionNotCreatedException("Unable to find matching provider for session"));
  }

  public static class Builder {
    private List<CapabilityMatcher> matchers = new LinkedList<>();
    private Comparator<Supplier<ActiveSession>> comparator = new DefaultProviderComparator();

    private Builder() {
    }

    public NewSessionPipeline build() {
      return new NewSessionPipeline(comparator, matchers);
    }

    public Builder match(CapabilityMatcher matcher) {
      matchers.add(Objects.requireNonNull(matcher, "Matcher must not be null"));
      return this;
    }

    public Builder orderedBy(Comparator<Supplier<ActiveSession>> comparator) {
      this.comparator = Objects.requireNonNull(comparator, "Comparator must not be null");
      return this;
    }

  }

}
