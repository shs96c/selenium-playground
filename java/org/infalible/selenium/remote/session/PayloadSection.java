package org.infalible.selenium.remote.session;

import java.util.Map;
import java.util.Objects;

class PayloadSection {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PayloadSection)) return false;
    PayloadSection that = (PayloadSection) o;
    return Objects.equals(getCapabilities(), that.getCapabilities()) &&
        Objects.equals(getMetadata(), that.getMetadata());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCapabilities(), getMetadata());
  }
}
