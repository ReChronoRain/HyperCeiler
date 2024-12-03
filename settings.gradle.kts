@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }
        maven{ url = uri("https://maven.aliyun.com/repository/google") }
        //maven{ url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven{ url = uri("https://maven.aliyun.com/repository/public") }
        maven{ url = uri("https://maven.aliyun.com/repository/jcenter") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }
        maven{ url = uri("https://maven.aliyun.com/repository/google") }
        //maven{ url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven{ url = uri("https://maven.aliyun.com/repository/public") }
        maven{ url = uri("https://maven.aliyun.com/repository/jcenter") }
        google()
        mavenCentral()
    }
}

rootProject.name = "HyperCeiler"
include(":app", ":hidden-api", ":app:processor")
