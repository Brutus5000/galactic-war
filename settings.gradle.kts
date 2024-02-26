pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "galactic-war"
include(":gw-wrapper")
include(":fa-gpgnet")
include(":fa-game-mock")
include(":fa-client-mock")