package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableList;
import org.openqa.selenium.SessionNotCreatedException;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class NewSessionPipeline {

  private final ImmutableList<CapabilityMatcher> matchers;

  private NewSessionPipeline(
      List<CapabilityMatcher> matchers) {
    this.matchers = ImmutableList.copyOf(matchers);
  }

  public static Builder builder() {
    return new Builder();
  }

  public ActiveSession newSession(NewSessionPayload payload) {
    return payload.stream()
        // Find all the possible providers of sessions
        .flatMap(section -> matchers.stream()
            .map(matcher -> matcher.match(section.getCapabilities(), section.getMetadata()))
            .filter(Objects::nonNull))
        // And now call each of them in turn. Eventually, we'll end up with a session
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

    private Builder() {
      // Not to be exposed to users
    }

    public NewSessionPipeline build() {
      return new NewSessionPipeline(matchers);
    }

    public Builder match(CapabilityMatcher matcher) {
      matchers.add(Objects.requireNonNull(matcher, "Matcher must not be null"));
      return this;
    }
  }
}
