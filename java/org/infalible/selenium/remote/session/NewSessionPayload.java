package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.openqa.selenium.SessionNotCreatedException;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.infalible.selenium.remote.session.Validators.IS_BOOLEAN;
import static org.infalible.selenium.remote.session.Validators.IS_PAGE_LOADING_STRATEGY;
import static org.infalible.selenium.remote.session.Validators.IS_PROXY;
import static org.infalible.selenium.remote.session.Validators.IS_STRING;
import static org.infalible.selenium.remote.session.Validators.IS_TIMEOUT;
import static org.infalible.selenium.remote.session.Validators.IS_UNHANDLED_PROMPT_BEHAVIOR;

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
  // Dedicate up to 10% of max ram to holding the payload
  private static final long THRESHOLD = Runtime.getRuntime().maxMemory() / 10;

  private final PayloadView view;

  public NewSessionPayload(Reader in, int estimatedLength) throws IOException {
    if (estimatedLength > THRESHOLD || Runtime.getRuntime().freeMemory() < estimatedLength) {
      this.view = new DiskBackedPayloadView(in);
    } else {
      this.view = new InMemoryPayloadView(in);
    }

    validate(this.view);
  }

  private void validate(PayloadView view) {
    extractW3CCapabilities(view).forEach(
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
    if (view instanceof Closeable) {
      ((Closeable) view).close();
    }
  }

  public Stream<PayloadSection> stream() {
    // Pick out the pieces
    ImmutableSortedMap<String, Object> metadata = view.getKeys()
        .filter(key -> !DEFINITELY_NOT_METADATA.contains(key))
        .map(view::getMetadata)
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .collect(ImmutableSortedMap.toImmutableSortedMap(
            Ordering.natural(),
            PayloadView.Entry::getKey,
            PayloadView.Entry::getValue));

    return Stream.concat(
        extractOssCapabilities(view),
        extractW3CCapabilities(view))
        .filter(Objects::nonNull)
        .map(caps -> new PayloadSection(caps, metadata));
  }


  private Stream<Map<String, Object>> extractOssCapabilities(PayloadView view) {
    return new JwpToW3CCapabilitiesAdapter().apply(view.getDesiredCapabilities());
  }

  private Stream<Map<String, Object>> extractW3CCapabilities(PayloadView view) {
    if (!view.containsKey("capabilities")) {
      return Stream.of();
    }

    Map<String, Object> alwaysMatch = view.getAlwaysMatch();
    Set<String> alwaysMatchKeys = alwaysMatch.keySet();

    return view.getFirstMatches()
        .peek(map -> {
          Set<String> duplicates = Sets.intersection(alwaysMatchKeys, map.keySet());
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
}
