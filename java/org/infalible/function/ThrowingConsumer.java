package org.infalible.function;

import java.util.function.Consumer;

@SuppressWarnings("FunctionalInterfaceMethodChanged")
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

  void acceptThrowing(T arg) throws Exception;

  default void accept(T arg) {
    try {
      acceptThrowing(arg);
    } catch (Throwable throwable) {
      onException(arg, throwable);
    }
  }

  default void onException(T arg, Throwable throwable) {
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }
    throw new RuntimeException(throwable);
  }
}
