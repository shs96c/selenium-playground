package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.openqa.selenium.SessionNotCreatedException;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static org.infalible.selenium.remote.json.Json.TO_MAP;

class InMemoryPayloadView extends PayloadView {
  private final Map<String, Object> completePayload;

  InMemoryPayloadView(Reader in) throws IOException {
    completePayload = TO_MAP.apply(CharStreams.toString(in));
  }

  @Override
  public Stream<String> getKeys() {
    return completePayload.keySet().stream();
  }

  @Override
  public Entry getMetadata(String key) {
    return new Entry(key, completePayload.get(key));
  }

  @Override
  public Map<String, Object> getDesiredCapabilities() {
    if (!completePayload.containsKey("desiredCapabilities")) {
      return null;
    }
    return coerceToMap(completePayload.get("desiredCapabilities"));
  }

  @Override
  public Map<String, Object> getAlwaysMatch() {
    Map<String, Object> allCaps = coerceToMap(completePayload.get("capabilities"));

    return coerceToMap(allCaps.getOrDefault("alwaysMatch", ImmutableMap.of()));
  }

  @Override
  public Stream<Map<String, Object>> getFirstMatches() {
    Map<String, Object> allCaps = coerceToMap(completePayload.get("capabilities"));

    Object raw = allCaps.getOrDefault("firstMatch", ImmutableList.of(ImmutableMap.of()));
    if (raw == null) {
      return ImmutableList.<Map<String, Object>>of(ImmutableMap.of()).stream();
    }

    if (!(raw instanceof Collection)) {
      throw new SessionNotCreatedException("Expected firstMatch value to be a list: " + raw);
    }

    Collection<?> firsts = (Collection<?>) raw;
    if (firsts.isEmpty()) {
      throw new SessionNotCreatedException("Expected firstMatch to have at least one value: " + raw);
    }

    return firsts.stream().map(this::coerceToMap);
  }

}
