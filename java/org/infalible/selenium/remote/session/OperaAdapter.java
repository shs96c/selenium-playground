package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

public class OperaAdapter implements CapabilitiesAdapter {
  @Override
  public Map<String, Object> apply(Map<String, Object> unmodifiedCaps) {
    ImmutableMap<String, Object> caps = unmodifiedCaps.entrySet().parallelStream()
        .filter(entry ->
            ("browserName".equals(entry.getKey()) && "opera".equals(entry.getValue())) ||
            ("browserName".equals(entry.getKey()) && "operablink".equals(entry.getValue())) ||
            "operaOptions".equals(entry.getKey()))
        .distinct()
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    return caps.isEmpty() ? null : caps;
  }
}
