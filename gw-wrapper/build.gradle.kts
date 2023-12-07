import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
}

dependencies {
    kapt(libs.picocli.codegen)
    implementation(libs.picocli)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    named<ShadowJar>("shadowJar") {
        manifest {
            attributes(mapOf("Main-Class" to "com.faforever.gw.GwWrapperApplication"))
        }
    }
}
