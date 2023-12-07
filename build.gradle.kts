import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.test.logger) apply false
}

subprojects {
    group = "com.faforever.gw"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    apply(plugin = "com.adarshr.test-logger")
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        val ktlintVersion = "1.0.1"
        kotlin {
            ktlint(ktlintVersion)
        }
        kotlinGradle {
            target("*.gradle.kts")

            ktlint(ktlintVersion)
        }
    }
}