package org.infalible.selenium.remote.session;

import org.infalible.function.NullableFunction;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.ErrorCodes;
import org.openqa.selenium.remote.Response;

import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class W3CResponseConverter implements Function<Map<String, Object>, Map.Entry<Dialect, Response>> {

  @Override
  public Map.Entry<Dialect, Response> apply(Map<String, Object> data) {
    if (data.get("status") instanceof Number || !(data.get("value") instanceof Map)) {
      return null;
    }

    @SuppressWarnings("unchecked") Map<String, Object> value = (Map<String, Object>) data.get("value");

    return new Failure().onNull(new Success()).andThen(response ->  {
      if (response == null) {
        return null;
      }
      return new AbstractMap.SimpleImmutableEntry<>(Dialect.W3C, response);
    }).apply(value);
  }

  private String asString(Object obj) {
    if (obj == null) {
      return null;
    }

    return String.valueOf(obj);
  }

  private class Success implements NullableFunction<Map<String, Object>, Response> {
    @Override
    public Response apply(Map<String, Object> data) {
      String sessionId = asString(data.get("sessionId"));
      Object rawCaps = data.get("capabilities");

      if (sessionId == null || !(rawCaps instanceof Map)) {
        return null;
      }

      Response response = new Response();
      response.setSessionId(sessionId);
      response.setStatus(ErrorCodes.SUCCESS);
      response.setState("success");
      response.setValue(rawCaps);

      return response;
    }
  }

  private class Failure implements NullableFunction<Map<String, Object>, Response> {
    private final ErrorCodes errorCodes = new ErrorCodes();

    @Override
    public Response apply(Map<String, Object> data) {
      String error = asString(data.get("error"));
      String message = asString(data.get("message"));
      String stacktrace = asString(data.get("stacktrace"));

      if (error == null) {
        return null;
      }

      WebDriverException exception = createException(error, message);
      if (stacktrace != null) {
        exception.addInfo("Remote stacktrace", stacktrace);
      }

      Response response = new Response();
      response.setValue(exception);
      response.setState(error);
      response.setStatus(errorCodes.toStatus(error, Optional.empty()));

      return response;
    }

    private WebDriverException createException(String error, String message) {
      Class<? extends WebDriverException> clazz = errorCodes.getExceptionType(error);

      try {
        Constructor<? extends WebDriverException> constructor = clazz.getConstructor(String.class);
        return constructor.newInstance(message);
      } catch (ReflectiveOperationException e) {
        throw new WebDriverException(message);
      }
    }
  }
}
