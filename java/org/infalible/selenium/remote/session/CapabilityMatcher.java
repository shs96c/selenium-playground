package org.infalible.selenium.remote.session;

import java.util.Map;
import java.util.function.Supplier;

@FunctionalInterface
public interface CapabilityMatcher {
  Supplier<ActiveSession> match(Map<String, Object> capabilities, Map<String, Object> metaInfo);
}
