pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.lsposed.lsparanoid") version "0.6.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.xposed.info")
        }
        maven("https://jitpack.io")

    }
}

rootProject.name = "HyperCeiler"
include(":app")
include(":hidden-api")
