@file:Suppress("UnstableApiUsage")
var gprUser = System.getenv("GIT_ACTOR") ?:""
var gprKey = System.getenv("GIT_TOKEN") ?: ""

val gprInfoFile = File(rootProject.projectDir, "signing.properties")

if (gprUser.isEmpty() || gprKey.isEmpty()) {
    if (gprInfoFile.exists()) {
        val gprInfo = java.util.Properties().apply {
            gprInfoFile.inputStream().use { load(it) }
        }

        // 在构建时请在 signing.properties 中添加 gpr.user（GitHub 用户名）和 gpr.key（GitHub 个人令牌密钥）
        // 提交时请勿提交以上字段，以免个人账号泄露
        //
        // When building, add gpr.user (GitHub username) and gpr.key (GitHub personal access token) to signing.properties.
        // Do not commit these fields to version control to avoid leaking personal account information.
        gprUser = gprInfo.getProperty("gpr.user") ?: ""
        gprKey = gprInfo.getProperty("gpr.key") ?: ""

        if (gprUser.isEmpty() || gprKey.isEmpty()) {
            throw GradleException("\'gpr.user\' and \'gpr.key\' must be set in \'signing.properties\'")
        }
    }
}

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

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/ReChronoRain/HyperCeiler")
            credentials {
                username = gprUser
                password = gprKey
            }
        }
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
    }
}

rootProject.name = "HyperCeiler"
include("app")
include(":library:common-ui", ":library:hook", "library:processor", "library:hidden-api")
