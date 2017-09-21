package org.infalible.selenium.w3c;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import org.openqa.selenium.Capabilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class W3CCapabilities {

  private final Capabilities alwaysMatch;
  private final List<Capabilities> firstMatches;
  private final Map<String, Object> metadata;

  private W3CCapabilities(Capabilities alwaysMatch, List<Capabilities> firstMatches, Map<String, Object> metadata) {
    this.alwaysMatch = alwaysMatch;
    this.firstMatches = firstMatches;
    this.metadata = metadata;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private final ImmutableSortedMap.Builder<String, Object> metadata = ImmutableSortedMap.naturalOrder();
    private final ImmutableList.Builder<Capabilities> firstMatches = ImmutableList.builder();
    private Capabilities alwaysMatch;

    private Builder() {
      // We don't want users just creating one of these things.
    }

    public Builder alwaysMatch(Capabilities caps) {
      this.alwaysMatch = Objects.requireNonNull(caps, "If set, alwaysMatch capabilities must not be null");
      return this;
    }

    public Builder addFirstMatches(Capabilities... caps) {
      firstMatches.add(Objects.requireNonNull(caps, "First match capabilities must not be null"));
      return this;
    }

    public Builder addFirstMatches(Collection<Capabilities> caps) {
      Objects.requireNonNull(caps, "First match capabilities must not be null");
      caps.forEach(this::addFirstMatches);
      return this;
    }

    public Builder addMetadata(String key, Object value) {
      Objects.requireNonNull(key, "Key is not set");
      Objects.requireNonNull(value, "Value is not set");

      if ("capabilities".equals(key)) {
        throw new IllegalArgumentException("Set w3c capabilities using firstMatch and alwaysMatch");
      }

      if ("desiredCapabilities".equals(key)) {
        throw new IllegalArgumentException("Set json wire protocol capabilities using setCapabilities");
      }

      metadata.put(key, value);
      return this;
    }

    public W3CCapabilities build() {
      return new W3CCapabilities(alwaysMatch, firstMatches.build(), metadata.build());
    }
  }

}