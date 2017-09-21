package org.infalible.selenium.remote.session;

import org.openqa.selenium.SessionNotCreatedException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

abstract class PayloadView {
  abstract Stream<String> getKeys();

  abstract Entry getMetadata(String key);

  boolean containsKey(String key) {
    return getKeys().anyMatch(key::equals);
  }

  abstract Map<String,Object> getAlwaysMatch();

  abstract Stream<Map<String, Object>> getFirstMatches();

  abstract Map<String, Object> getDesiredCapabilities();

  protected Map<String, Object> coerceToMap(Object value) {
    if (!(value instanceof Map)) {
      throw new SessionNotCreatedException("Expected value to be a map: " + value);
    }

    // Validate that all keys are strings.
    for (Object key : ((Map<?, ?>) value).keySet()) {
      if (!(key instanceof String)) {
        throw new SessionNotCreatedException("Key was not a string: " + key);
      }
    }

    @SuppressWarnings("unchecked") Map<String, Object> toReturn = (Map<String, Object>) value;
    return toReturn;
  }

  class Entry {
    private final String key;
    private final Object value;

    public Entry(String key, Object value) {
      this.key = Objects.requireNonNull(key);
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }
}
