package org.infalible.selenium.json;

import com.google.gson.JsonParseException;
import org.openqa.selenium.WebDriverException;

public class JsonException extends WebDriverException {
  public JsonException(JsonParseException jpe) {
    super(jpe.getMessage(), jpe.getCause());
    setStackTrace(jpe.getStackTrace());
  }
}
