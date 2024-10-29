@file:Suppress("UnstableApiUsage")

include(":miuix")


pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info")
        maven("https://jitpack.io")
    }
}

rootProject.name = "HyperCeiler"
include(":app", ":hidden-api")
include(":app", ":miuistub")
include(":app", ":app:processor")
