pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "zz143"

include(":zz143-core")
include(":zz143-capture")
include(":zz143-learn")
include(":zz143-suggest")
include(":zz143-replay")
include(":zz143-android")
include(":demo-app")
