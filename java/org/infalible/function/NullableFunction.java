package org.infalible.function;

import java.util.Objects;
import java.util.function.Function;

public interface NullableFunction<T, R> extends Function<T, R> {

  default NullableFunction<T, R> onNull(Function<T, R> callThis) {
    Objects.requireNonNull(callThis);

    return (T t) -> {
      R r = apply(t);
      if (r == null) {
        return callThis.apply(t);
      }
      return r;
    };
  };

}
