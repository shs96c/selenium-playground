java_test(
  name = 'AllTests',
  srcs = glob(['*.java']),
  deps = [
    '//java/org/infalible/selenium/json',
    "//java/org/infalible/selenium/remote/session",
    '//java/org/infalible/testing/junit',
    '//third-party/java/gson',
    '//third-party/java/guava',
    '//third-party/java/junit',
    '//third-party/java/selenium',
  ],
)

java_binary(
  name = 'spike',
  main_class = 'org.infalible.selenium.remote.session.Spike',
  srcs = [
    "Spike.java",
  ],
  deps = [
    '//java/org/infalible/selenium/json',
    '//java/org/infalible/selenium/remote/session',
    '//third-party/java/selenium',
  ]
)
