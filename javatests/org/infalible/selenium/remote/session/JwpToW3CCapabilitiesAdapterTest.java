package org.infalible.selenium.remote.session;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.openqa.selenium.remote.CapabilityType;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JwpToW3CCapabilitiesAdapterTest {

  @Test
  public void emptyCapabilitiesAreLeftAsEmptyCapabilities() {
    Map<String, Object> caps = ImmutableMap.of();

    Set<Map<String, Object>> converted = adapt(caps);

    assertEquals(1, converted.size());
    assertEquals(ImmutableSet.of(ImmutableMap.of()), converted);
  }

  @Test
  public void capabilitiesWithJustABrowserNameAreLeftAsIs() {
    Map<String, Object> caps = ImmutableMap.of(CapabilityType.BROWSER_NAME, "cheese");

    Set<Map<String, Object>> converted = adapt(caps);

    assertEquals(ImmutableSet.of(ImmutableMap.of(CapabilityType.BROWSER_NAME, "cheese")), converted);
  }

  @Test
  public void shouldBreakOutChromeAndFirefoxIntoSeparateBlobs() {
    Map<String, Object> caps = ImmutableMap.of(
        CapabilityType.BROWSER_NAME, "firefox",
        "goog:chromeOptions", ImmutableMap.of());

    Set<Map<String, Object>> converted = adapt(caps);

    assertEquals(converted.toString(), 2, converted.size());

    assertTrue(converted.contains(ImmutableMap.of("browserName", "firefox")));
    assertTrue(converted.contains(ImmutableMap.of("goog:chromeOptions", ImmutableMap.of())));
  }

  private Set<Map<String, Object>> adapt(Map<String, Object> caps) {
    JwpToW3CCapabilitiesAdapter adapter = new JwpToW3CCapabilitiesAdapter();
    return adapter.apply(caps).collect(ImmutableSet.toImmutableSet());
  }
}