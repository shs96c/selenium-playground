package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import org.openqa.selenium.SessionNotCreatedException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.remote.json.Json.TO_MAP;

public class NewSessionPayload implements Closeable {

  private final static Set<String> DEFINITELY_NOT_METADATA = ImmutableSet.of(
      "capabilities",
      "desiredCapabilities",
      "requiredCapabilities"
  );
  private final Map<String, Object> completePayload;

  public NewSessionPayload(InputStream in, int estimatedLength) throws IOException {
    byte[] bytes = ByteStreams.toByteArray(in);
    completePayload = TO_MAP.apply(new String(bytes, UTF_8));
  }

  @Override
  public void close() throws IOException {
    // Do nothing
  }

  public Stream<PayloadSection> stream() {
    // Pick out the pieces
    ImmutableSortedMap<String, Object> metadata = completePayload.entrySet().stream()
        .filter(entry -> Objects.nonNull(entry.getKey()))
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .filter(entry -> !DEFINITELY_NOT_METADATA.contains(entry.getKey()))
        .collect(ImmutableSortedMap.toImmutableSortedMap(Ordering.natural(), Map.Entry::getKey, Map.Entry::getValue));

    return Stream.concat(
        extractOssCapabilities(completePayload.get("desiredCapabilities")),
        extractW3CCapabilities(completePayload.get("capabilities")))
        .map(caps -> new PayloadSection(caps, metadata));
  }


  private Stream<Map<String, Object>> extractOssCapabilities(Object raw) {
    if (raw == null) {
      return Stream.of();
    }

    return Stream.of(coerceToMap(raw));
  }

  private Stream<Map<String, Object>> extractW3CCapabilities(Object raw) {
    if (raw == null) {
      return Stream.of();
    }

    Map<String, Object> capabilities = coerceToMap(raw);
    // The spec says it's legit to just have the key with no values in it.
    if (!capabilities.containsKey("firstMatch") && !capabilities.containsKey("alwaysMatch")) {
      return Stream.of(ImmutableMap.of());
    }

    Object value = capabilities.get("alwaysMatch");
    Map<String, Object> alwaysMatch = value == null ? ImmutableMap.of() : coerceToMap(value);

    value = capabilities.get("firstMatch");
    List<Object> firstMatch;
    if (value == null) {
      firstMatch = ImmutableList.of(ImmutableMap.of());
    } else {
      if (!(value instanceof List)) {
        throw new SessionNotCreatedException("First match capabilities were not a list: " + value);
      }
      firstMatch = (List<Object>) value;
      if (firstMatch.isEmpty()) {
        firstMatch = ImmutableList.of(ImmutableMap.of());
      }
    }

    return firstMatch.stream()
        .map(this::coerceToMap)
        .map(map -> {
          Map<String, Object> toReturn = new TreeMap<>();
          toReturn.putAll(alwaysMatch);
          toReturn.putAll(map);
          return toReturn;
        });
  }

  private Map<String, Object> coerceToMap(Object value) {
    if (!(value instanceof Map)) {
      throw new SessionNotCreatedException("Expected value to be a map: " + value);
    }

    // Validate that all keys are strings.
    ((Map<?, ?>) value).keySet().forEach(key -> {
      if (!(key instanceof String)) {
        throw new SessionNotCreatedException("Key was not a string: " + key);
      }
    });

    @SuppressWarnings("unchecked") Map<String, Object> toReturn = (Map<String, Object>) value;
    return toReturn;
  }
}
