plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.skie)
    id("signing")
}

// Single source of truth for the published version, mirroring every other kiit module.
// Published to mavenLocal only for now (./gradlew :kiit-logs:publishToMavenLocal).
val libraryVersion = "0.7.0"

kotlin {
    jvm {
        compilerOptions {
            // JVM 21 so Kotlin emits PermittedSubclasses for any sealed hierarchies, enabling
            // exhaustive Java pattern-matching `switch`, same as every other kiit KMP module.
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64(), iosX64(), macosArm64(), macosX64()).forEach {
        it.binaries.framework {
            baseName = "KiitLogs"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Disabled: SKIE's default analytics upload sends git/hardware/project data to Touchlab. Turn
// on only when that's something explicitly wanted, not because it's a default worth keeping.
skie {
    analytics {
        enabled.set(false)
    }
}

android {
    namespace = "kiit.logs"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        // android.util.Log is used by the console logger, without this it throws "not mocked" in unit tests
        unitTests.isReturnDefaultValues = true
    }
}

/**
 * Store the following in ~/.gradle/gradle.properties
 *
 * signingInMemoryKeyPassword=
 * signingInMemoryKey=
 * signing.gnupg.keyName=
 * signing.gnupg.passphrase=
 *
 * Maven local: ~/.m2/repository/dev/kiit/kiit-logs/
 */
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    coordinates(
        groupId = "dev.kiit",
        artifactId = "kiit-logs",
        version = libraryVersion,
    )
    pom {
        name = "kiit-logs"
        description = "Small Kotlin logging API with a console default and pluggable providers. Part of Kiit."
        url = "https://kiit.dev"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "codehelix"
                name = "CodeHelix"
                url = "https://kiit.dev"
            }
        }
        scm {
            url = "https://github.com/kiitdev/kiit-logs"
            connection = "scm:git:git://github.com/kiitdev/kiit-logs.git"
            developerConnection = "scm:git:ssh://git@github.com/kiitdev/kiit-logs.git"
        }
    }
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    source.setFrom(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/androidMain/kotlin",
        "src/appleMain/kotlin",
    )
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

// Publishing to Maven local doesn't need signatures, and gpg can't ask for its passphrase in a script.
// Signing is skipped only when the run has a Maven local publish and no Maven Central one.
tasks.withType<Sign>().configureEach {
    onlyIf {
        val names = gradle.taskGraph.allTasks.map { it.name }
        !(names.any { it.endsWith("ToMavenLocal") } && names.none { it.contains("MavenCentral") })
    }
}

// The jvm() target compiles to JVM 21 bytecode (see the jvm{} block above), so jvmTest needs to
// run on a matching JVM, same as every other kiit KMP module.
tasks.named<Test>("jvmTest") {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}

// Read by the release workflow (once one exists) to derive the git tag/GitHub release name from
// the same version published to Maven Central, same convention as every other kiit module.
tasks.register("printVersion") {
    doLast { println(libraryVersion) }
}
