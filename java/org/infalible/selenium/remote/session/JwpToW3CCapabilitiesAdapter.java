package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Presents a Json Wire Protocol Capability instance as a stream of W3C Capabilities for use with New Session
 */
public class JwpToW3CCapabilitiesAdapter implements Function<Map<String, Object>, Stream<Map<String, Object>>> {

  private final Set<CapabilitiesAdapter> adapters;

  public JwpToW3CCapabilitiesAdapter() {
    ImmutableSet.Builder<CapabilitiesAdapter> builder = ImmutableSet.<CapabilitiesAdapter>builder();

    ServiceLoader<CapabilitiesAdapter> loader = ServiceLoader.load(CapabilitiesAdapter.class);
    loader.forEach(builder::add);

    builder
        .add(new ChromeAdapter())
        .add(new EdgeAdapter())
        .add(new FirefoxAdapter())
        .add(new IEAdapter())
        .add(new OperaAdapter())
        .add(new SafariAdapter());

    this.adapters = builder.build();
  }

  public Stream<Map<String,Object>> apply(Map<String, Object> caps) {
    Set<String> usedKeys = new HashSet<>();

    // We're going to do this in two passes. The first will let us populate usedKeys, which we can then use to generate
    // the actual set of results we want to return to the user.

    ImmutableList<Map<String, Object>> generated = adapters.stream()
        .map(adapter -> adapter.apply(caps))
        .filter(Objects::nonNull)
        .peek(map -> usedKeys.addAll(map.keySet()))
        .collect(ImmutableList.toImmutableList());

    if (generated.isEmpty()) {
      return Stream.of(caps);
    }

    return generated.stream()
        .map(new RemainingKeys(usedKeys))
        .map(new CommonKeys(caps, usedKeys));
  }

  private static class RemainingKeys implements CapabilitiesAdapter {

    private final Set<String> usedKeys;

    RemainingKeys(Set<String> usedKeys) {
      this.usedKeys = usedKeys;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> unmodifedCaps) {
      ImmutableMap<String, Object> toReturn = unmodifedCaps.entrySet().stream()
          .filter(entry -> !usedKeys.contains(entry.getKey()))
          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

      // If `usedKeys` is empty, we were given an empty map at the start of the process. Nothing will have matched.
      return toReturn.isEmpty() && !usedKeys.isEmpty() ? unmodifedCaps : toReturn;
    }
  }

  private static class CommonKeys implements Function<Map<String, Object>, Map<String, Object>> {

    private static final Predicate<String> INJECTABLE =
        Stream.of(
                "^acceptInsecureCerts$",
                "^browserName$",
                "^browserVersion$",
                "^platformName$",
                "^pageLoadStrategy$",
                "^proxy$",
                "^setWindowRect$",
                "^se:.*$",          // Sneakily allow us to inject our own values
                "^timeouts$",
                "^unhandledPromptBehavior$")
            .map(Pattern::compile)
            .map(Pattern::asPredicate)
            .reduce(identity -> false, Predicate::or);

    private final Map<String, Object> injectedValues;

    CommonKeys(Map<String, Object> deriveFrom, Set<String> usedKeys) {
      this.injectedValues = deriveFrom.entrySet().stream()
          .filter(entry -> INJECTABLE.test(entry.getKey()))
          .filter(entry -> !usedKeys.contains(entry.getKey()))
          .filter(entry -> Objects.nonNull(entry.getValue()))
          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> in) {
      ImmutableSortedMap.Builder<String, Object> builder = ImmutableSortedMap.naturalOrder();
      builder.putAll(in);

      injectedValues.entrySet().stream()
          .filter(entry -> !in.containsKey(entry.getKey()))
          .forEach(builder::put);

      return builder.build();
    }
  }
}
