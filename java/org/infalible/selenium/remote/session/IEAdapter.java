package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

class IEAdapter implements CapabilitiesAdapter {
  @Override
  public Map<String, Object> apply(Map<String, Object> unmodifiedCaps) {
    ImmutableMap<String, Object> caps = unmodifiedCaps.entrySet().parallelStream()
        .filter(entry ->
            ("browserName".equals(entry.getKey()) && "internet explorer".equals(entry.getValue())) ||
            "browserAttachTimeout".equals(entry.getKey()) ||
            "enableElementCacheCleanup".equals(entry.getKey()) ||
            "enablePersistentHover".equals(entry.getKey()) ||
            "extractPath".equals(entry.getKey()) ||
            "host".equals(entry.getKey()) ||
            "ignoreZoomSetting".equals(entry.getKey()) ||
            "initialBrowserZoom".equals(entry.getKey()) ||
            "logFile".equals(entry.getKey()) ||
            "logLevel".equals(entry.getKey()) ||
            "requireWindowFocus".equals(entry.getKey()) ||
            "se:ieOptions".equals(entry.getKey()) ||
            "silent".equals(entry.getKey()) ||
            entry.getKey().startsWith("ie."))
        .distinct()
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    return caps.isEmpty() ? null : caps;
  }
}
