package org.infalible.selenium.remote.session;

import java.util.Map;

class IdentityAdapter implements CapabilitiesAdapter {
  @Override
  public Map<String, Object> apply(Map<String, Object> unmodifiedCaps) {
    return unmodifiedCaps;
  }
}
