package org.infalible.selenium.remote.session;

import com.google.common.primitives.Ints;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Dialect;
import org.openqa.selenium.remote.ErrorCodes;
import org.openqa.selenium.remote.Response;

import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class OssResponseConverter implements Function<Map<String, Object>, Map.Entry<Dialect, Response>> {

  private final ErrorCodes errorCodes = new ErrorCodes();

  @Override
  public Map.Entry<Dialect, Response> apply(Map<String, Object> data) {
    if (!(data.get("status") instanceof Number) ||
        !(data.get("value") instanceof Map) ||
        data.get("sessionId") == null) {
      return null;
    }

    Response response = new Response();
    response.setSessionId(String.valueOf(data.get("sessionId")));
    response.setStatus((((Number) data.get("status")).intValue()));

    Object value = data.get("value");
    if (response.getStatus() != ErrorCodes.SUCCESS && value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      WebDriverException exception = createException(response.getStatus(), String.valueOf(map.get("message")));

      if (map.get("stackTrace") instanceof Collection) {
        FrameInfoToStackFrame stackFrame = new FrameInfoToStackFrame();

        StackTraceElement[] stackTraces = ((Collection<?>) map.get("stackTrace")).stream()
            .filter(obj -> Map.class.isAssignableFrom(obj.getClass()))
            .map(obj -> stackFrame.apply((Map<?, ?>) obj))
            .filter(Objects::nonNull)
            .toArray(StackTraceElement[]::new);

        exception.setStackTrace(stackTraces);
      }

      value = exception;
    }

    response.setValue(value);

    return new AbstractMap.SimpleImmutableEntry<>(Dialect.OSS, response);
  }

  private WebDriverException createException(int errorCode, String message) {
    Class<? extends WebDriverException> clazz = errorCodes.getExceptionType(errorCode);

    try {
      Constructor<? extends WebDriverException> constructor = clazz.getConstructor(String.class);
      return constructor.newInstance(message);
    } catch (ReflectiveOperationException e) {
      throw new WebDriverException(message);
    }
  }

  private static class FrameInfoToStackFrame implements Function<Map<?, ?>, StackTraceElement> {
    public StackTraceElement apply(Map<?, ?> frameInfo) {
      if (frameInfo == null) {
        return null;
      }

      Optional<Number> maybeLineNumberInteger = Optional.empty();

      final Object lineNumberObject = frameInfo.get("lineNumber");
      if (lineNumberObject instanceof Number) {
        maybeLineNumberInteger = Optional.of((Number) lineNumberObject);
      } else if (lineNumberObject != null) {
        // might be a Number as a String
        maybeLineNumberInteger = Optional.ofNullable(Ints.tryParse(lineNumberObject.toString()));
      }

      // default -1 for unknown, see StackTraceElement constructor javadoc
      final int lineNumber = maybeLineNumberInteger.orElse(-1).intValue();

      // Gracefully handle remote servers that don't (or can't) send back
      // complete stack trace info. At least some of this information should
      // be included...
      String className = frameInfo.containsKey("className") ?
          toStringOrNull(frameInfo.get("className")) : "<anonymous class>";
      String methodName = frameInfo.containsKey("methodName") ?
          toStringOrNull(frameInfo.get("methodName")) : "<anonymous method>";
      String fileName = frameInfo.containsKey("fileName") ?
          toStringOrNull(frameInfo.get("fileName")) : null;

      return new StackTraceElement(
          className,
          methodName,
          fileName,
          lineNumber);
    }

    private static String toStringOrNull(Object o) {
      return o == null ? null : o.toString();
    }
  }
}
