package org.infalible.selenium.remote;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infalible.selenium.remote.Validators.IS_BOOLEAN;
import static org.infalible.selenium.remote.Validators.IS_PAGE_LOADING_STRATEGY;
import static org.infalible.selenium.remote.Validators.IS_PROXY;
import static org.infalible.selenium.remote.Validators.IS_STRING;
import static org.infalible.selenium.remote.Validators.IS_TIMEOUT;
import static org.infalible.selenium.remote.Validators.IS_UNHANDLED_PROMPT_BEHAVIOR;
import static org.infalible.selenium.remote.json.Json.TO_MAP;

public class NewSessionPayload implements Closeable {

  private final static Predicate<String> ACCEPTED_W3C_PATTERNS = Stream.of(
      "^[\\w-]+:.*$",
      "^acceptInsecureCerts$",
      "^browserName$",
      "^browserVersion$",
      "^platformName$",
      "^pageLoadStrategy$",
      "^proxy$",
      "^setWindowRect$",
      "^timeouts$",
      "^unhandledPromptBehavior$")
      .map(Pattern::compile)
      .map(Pattern::asPredicate)
      .reduce(identity -> false, Predicate::or);

  private final static Map<String, Predicate<Object>> TYPE_CHECKS = ImmutableMap.<String, Predicate<Object>>builder()
      .put("acceptInsecureCerts", IS_BOOLEAN)
      .put("browserName", IS_STRING)
      .put("browserVersion", IS_STRING)
      .put("platformName", IS_STRING)
      .put("pageLoadingStrategy", IS_PAGE_LOADING_STRATEGY)
      .put("proxy", IS_PROXY)
      .put("setWindowRect", IS_BOOLEAN)
      .put("timeouts", IS_TIMEOUT)
      .put("unhandledPromptBehavior", IS_UNHANDLED_PROMPT_BEHAVIOR)
      .build();

  private final static Set<String> DEFINITELY_NOT_METADATA = ImmutableSet.of(
      "capabilities",
      "desiredCapabilities",
      "requiredCapabilities"
  );
  private final Map<String, Object> completePayload;

  public NewSessionPayload(InputStream in, int estimatedLength) throws IOException {
    byte[] bytes = ByteStreams.toByteArray(in);
    completePayload = TO_MAP.apply(new String(bytes, UTF_8));

    validate(completePayload.get("capabilities"));
  }

  private void validate(Object capabilities) {
    if (capabilities == null) {
      return;
    }

    extractW3CCapabilities(capabilities).forEach(
            map -> {
              for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!ACCEPTED_W3C_PATTERNS.test(entry.getKey())) {
                  throw new SessionNotCreatedException("Illegal key value seen: " + entry.getKey());
                }

                if (!TYPE_CHECKS.getOrDefault(entry.getKey(), obj -> true).test(entry.getValue())) {
                  throw new SessionNotCreatedException(
                      String.format(
                          "Unexpected value seen for %s - %s",
                          entry.getKey(),
                          entry.getValue()));
                }
              }
            });
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
    List<?> firstMatch;
    if (value == null) {
      firstMatch = ImmutableList.of(ImmutableMap.of());
    } else {
      if (!(value instanceof List)) {
        throw new SessionNotCreatedException("First match capabilities were not a list: " + value);
      }
      firstMatch = (List<?>) value;
      if (firstMatch.isEmpty()) {
        firstMatch = ImmutableList.of(ImmutableMap.of());
      }
    }

    Set<String> alwaysKeys = alwaysMatch.keySet();
    return firstMatch.stream()
        .map(this::coerceToMap)
        .peek(match -> {
          Set<String> duplicates = Sets.intersection(alwaysKeys, match.keySet());
          if (!duplicates.isEmpty()) {
            throw new SessionNotCreatedException("Duplicate keys seen in w3c payload: " + duplicates);
          }
        })
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
