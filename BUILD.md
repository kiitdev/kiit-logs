# kiit-logs — Build Guide

All Gradle commands below are run from the **repository root**.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK  | 17+     | `java -version` to verify |
| Android SDK | any | Required for `androidTarget` compilation |
| Xcode | current | Required for the iOS targets (macOS only) |
| GPG  | 2.x     | Signing uses `useGpgCmd()`, which shells out to the system `gpg` |

## Build and test

```bash
./gradlew build
./gradlew :kiit-logs:jvmTest
./gradlew :kiit-logs:iosSimulatorArm64Test
./gradlew :kiit-logs:testDebugUnitTest
./gradlew :kiit-logs:ktlintCheck :kiit-logs:detekt
```

## Publish to Maven local

```bash
./gradlew :kiit-logs:publishToMavenLocal
```

Artifacts land in `~/.m2/repository/dev/kiit/kiit-logs*/`. Consume them with `mavenLocal()` in the
consuming build's `repositories` block.

Publishing to Maven Central is not set up yet.
