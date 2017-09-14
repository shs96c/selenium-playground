package org.infalible.selenium.remote.session;

public class FakeSession implements ActiveSession {
  private final String name;

  public FakeSession(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "FakeSession(" + name + ")";
  }
}
