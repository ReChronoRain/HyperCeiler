@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        maven("https://mirrors.tuna.tsinghua.edu.cn/maven/")
        maven("https://maven.aliyun.com/repository/google")
        // maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
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
        maven("https://mirrors.tuna.tsinghua.edu.cn/maven/")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "HyperCeiler"
include(":app", ":hidden-api", ":app:processor")
