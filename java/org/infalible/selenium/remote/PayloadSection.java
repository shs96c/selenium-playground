package org.infalible.selenium.remote;

import java.util.Map;
import java.util.Objects;

public class PayloadSection {
  private final Map<String, Object> capabilities;
  private final Map<String, Object> metadata;

  public PayloadSection(Map<String, Object> capabilities, Map<String, Object> metadata) {

    this.capabilities = Objects.requireNonNull(capabilities, "Capabilities must be set");
    this.metadata = Objects.requireNonNull(metadata, "Metadata must be set");
  }

  public Map<String, Object> getCapabilities() {
    return capabilities;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }
}
