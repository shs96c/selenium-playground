package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableSet;
import org.openqa.selenium.Proxy;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

class Validators {
  private final static Set<String> PAGE_LOADING_STRATEGIES = ImmutableSet.of(
    "eager",
    "none",
    "normal");
  private final static Set<String> UNHANDLED_PROMPT_BEHAVIORS = ImmutableSet.of(
      "accept",
      "accept and notify",
      "dismiss",
      "dismiss and notify",
      "ignore");
  private final static Set<String> TIMEOUT_TYPES = ImmutableSet.of(
      "implicit",
      "pageLoad",
      "script");

  static final Predicate<Object> IS_BOOLEAN = obj -> obj instanceof Boolean;
  static final Predicate<Object> IS_PAGE_LOADING_STRATEGY = PAGE_LOADING_STRATEGIES::contains;
  @SuppressWarnings("unchecked")
  static final Predicate<Object> IS_PROXY = obj -> {
    try {
      new Proxy((Map<String, ?>) obj);
      return true;
    } catch (Exception e) {
      return false;
    }
  };
  static final Predicate<Object> IS_STRING = obj -> obj instanceof String;
  static final Predicate<Object> IS_TIMEOUT = obj -> {
    if (!(obj instanceof Map)) {
      return false;
    }
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
      if (!TIMEOUT_TYPES.contains(entry.getKey())) {
        return false;
      }
      if (!(entry instanceof Number)) {
        return false;
      }
    }
    return true;
  };
  static final Predicate<Object> IS_UNHANDLED_PROMPT_BEHAVIOR = UNHANDLED_PROMPT_BEHAVIORS::contains;
}
