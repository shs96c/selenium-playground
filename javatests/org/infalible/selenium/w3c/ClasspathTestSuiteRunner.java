package org.infalible.selenium.w3c;

import com.google.common.reflect.ClassPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class ClasspathTestSuiteRunner extends Suite {

  public ClasspathTestSuiteRunner(Class<?> clazz, RunnerBuilder builder) throws InitializationError {
    super(builder, findClasses(clazz));
  }

  private static Class<?>[] findClasses(Class<?> callingClass) {
    try {
      Set<Class<?>> testClasses = new TreeSet<>(Comparator.comparing(Class::getName));

      ClassPath classPath = ClassPath.from(ClasspathTestSuiteRunner.class.getClassLoader());
      for (ClassPath.ClassInfo info : classPath.getTopLevelClasses()) {
        if (!info.getPackageName().startsWith(callingClass.getPackage().getName())) {
          continue;
        }

        Class<?> clazz;
        try {
          clazz = Class.forName(info.getName());
        } catch (Throwable e) {
          continue;
        }

        if (clazz.equals(callingClass)) {
          continue;
        }

        // If the class is annotated with a @RunWith, then it's a test
        if (clazz.getAnnotation(RunWith.class) != null) {
          testClasses.add(clazz);
          continue;
        }

        // Otherwise, if any of the fields are annotated as being @Test, then it's a test
        for (Method method : clazz.getMethods()) {
          if (method.getAnnotation(Test.class) != null) {
            testClasses.add(clazz);
            break;
          }
        }
      }

      return testClasses.toArray(new Class<?>[testClasses.size()]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
