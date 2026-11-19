@file:Suppress("UnstableApiUsage")

data class GprCredentials(val user: String, val key: String)

fun loadGprCredentials(): GprCredentials? {
    // 优先从环境变量读取
    val envUser = System.getenv("GIT_ACTOR")?.takeIf { it.isNotBlank() }
    val envKey = System.getenv("GIT_TOKEN")?.takeIf { it.isNotBlank() }

    if (envUser != null && envKey != null) {
        return GprCredentials(envUser, envKey)
    }

    // 从 signing.properties 读取
    val propsFile = File(rootDir, "signing.properties")
    if (!propsFile.isFile) return null

    val props = java.util.Properties().apply {
        propsFile.inputStream().use { load(it) }
    }

    val user = props.getProperty("gpr.user")?.takeIf { it.isNotBlank() } ?: return null
    val key = props.getProperty("gpr.key")?.takeIf { it.isNotBlank() } ?: return null

    return GprCredentials(user, key)
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
        // Mirrors first: several artifacts on google()/mavenCentral() are redirected
        // to github.com release assets, which is unreachable here and stalls the build.
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.github.com/ReChronoRain/HyperCeiler") {
            // Keep the repository available for cached dependencies without credentials.
            val gpr = loadGprCredentials()
            if (gpr != null) {
                credentials {
                    username = gpr.user
                    password = gpr.key
                }
            } else {
                content {
                    includeGroup("com.sevtinge.hyperceiler")
                    includeGroup("fan.miuix")
                }
            }
        }
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
    }
}

rootProject.name = "HyperCeiler"

include(
    "app",
    // ":library:hook",
    ":library:libhook",
    // ":library:xposed-api-101",
    ":library:core",
    ":library:provision",
    ":library:common",
    ":library:processor",
    ":library:hidden-api",
)
