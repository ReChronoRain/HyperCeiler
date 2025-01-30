@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    // 在构建时请在 gradle.properties 中添加 gpr.user（github 用户名）和 gpr.key（GitHub 个人令牌密钥）
    // 提交时请勿提交以上字段，以免个人账号泄露
    //
    // When constructing, add GPR.USER (Github user name) and GPR.KEY (Github personal token key) to Gradle.properties.
    // Do not submit the above fields when submitted to avoid leakage of personal accounts
    val gprUser = settings.providers.gradleProperty("gpr.user")
    val gprKey = settings.providers.gradleProperty("gpr.key")
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/ReChronoRain/HyperCeiler")
            credentials {
                username = gprUser.orNull ?: System.getenv("GIT_ACTOR")
                password = gprKey.orNull ?: System.getenv("GIT_TOKEN")
            }
        }
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
    }
}

rootProject.name = "HyperCeiler"
include("app", "provision", "processor", "hidden-api")
