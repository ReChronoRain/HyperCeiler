@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        //maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }
        //maven { url = uri("https://maven.aliyun.com/repository/public/") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }
        //maven { url = uri("https://maven.aliyun.com/repository/public/") }
        google()
        mavenCentral()
        maven("https://api.xposed.info")
        maven("https://jitpack.io")
    }
}

rootProject.name = "HyperCeiler"
include(":app", ":hidden-api", ":app:processor")
