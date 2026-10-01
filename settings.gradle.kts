pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Lets Gradle auto-provision a JDK toolchain for compiling/testing when only an older JDK is
    // installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kiit-logs"

include(":kiit-logs")
include(":samples:sample-kotlin")
