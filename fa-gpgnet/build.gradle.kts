plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.guava)
    implementation(libs.jackson.annotations)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.faforever.fa"
            artifactId = "fa-gpgnet"

            from(components["java"])
        }
    }
}
