# kiit-logs — Build Guide

All Gradle commands below are run from the **repository root**.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK  | 17+     | `java -version` to verify. A JDK 21 toolchain is used for the JVM target and is downloaded if missing |
| Android SDK | any | Required for `androidTarget` compilation |
| Xcode | current | Required for the iOS targets and their tests (macOS only) |
| GPG  | 2.x     | Only for signed releases. `signing { useGpgCmd() }` shells out to the system `gpg`. Not needed for Maven local |

## Build and test

```bash
./gradlew build                            # everything: compile, lint, tests on all targets
./gradlew :kiit-logs:jvmTest               # JVM, includes the concurrency tests
./gradlew :kiit-logs:testDebugUnitTest     # Android, run on the JVM
./gradlew :kiit-logs:iosSimulatorArm64Test # iOS simulator, macOS only
./gradlew :kiit-logs:ktlintCheck :kiit-logs:detekt

# Run the Kotlin sample app
./gradlew :samples:sample-kotlin:run
```

`ktlintFormat` fixes most formatting problems: `./gradlew :kiit-logs:ktlintFormat`.

## Publish to Maven local

```bash
./gradlew :kiit-logs:publishToMavenLocal
```

Artifacts land in `~/.m2/repository/dev/kiit/kiit-logs*/0.7.0/`. Consume them with `mavenLocal()` in the
consuming build's `repositories` block. Signing is skipped for this task, so no gpg key is needed.

Publishing to Maven Central is not set up yet, and nothing in this repository publishes to it.
