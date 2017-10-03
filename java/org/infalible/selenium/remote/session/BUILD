java_library(
    name = "session",
    srcs = glob(["*.java"]),
    deps = [
        "//java/org/infalible/function",
        "//java/org/infalible/selenium/json",
        "//java/org/infalible/selenium/w3c",
        "//third-party/java/gson",
        "//third-party/java/guava",
        "//third-party/java/selenium",
    ],
    visibility = [
        "//javatests/org/infalible/selenium/remote/session:__pkg__",
    ]
)

java_binary(
    name = "new-session",
    main_class = "org.infalible.selenium.remote.session.NewSessionRequest",
    runtime_deps = [
        ":session",
    ]
)