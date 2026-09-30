// Root aggregator, no dependencies of its own.
// The library lives in :kiit-logs, demo apps live under :samples.
plugins {
    idea
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.skie) apply false
}

// Keeps IntelliJ from indexing the ports and the non-Gradle samples.
// Gradle itself already ignores them, they aren't included in settings.gradle.kts.
idea {
    module {
        excludeDirs.addAll(
            listOf(
                file("ports"),
                file("samples/sample-ts"),
                file("samples/sample-swift"),
            ),
        )
    }
}
