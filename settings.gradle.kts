@file:Suppress("UnstableApiUsage")


pluginManagement {
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        google()
        mavenCentral()
        flatDir {
            dirs("provision/libs")
        }
    }
}

rootProject.name = "HyperCeiler"
include(":app", "provision", "processor", "hidden-api")