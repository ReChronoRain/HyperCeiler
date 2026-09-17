@file:Suppress("UnstableApiUsage")

data class GprCredentials(val user: String, val key: String)

fun loadGprCredentials(): GprCredentials? {
    // Prefer environment variables when available.
    val envUser = System.getenv("GIT_ACTOR")?.takeIf { it.isNotBlank() }
    val envKey = System.getenv("GIT_TOKEN")?.takeIf { it.isNotBlank() }

    if (envUser != null && envKey != null) {
        return GprCredentials(envUser, envKey)
    }

    // Fall back to signing.properties for local/release builds.
    val propsFile = File(rootDir, "signing.properties")
    if (!propsFile.exists()) {
        return null
    }

    val props = java.util.Properties().apply {
        propsFile.inputStream().use { load(it) }
    }

    val user = props.getProperty("gpr.user")?.takeIf { it.isNotBlank() }
    val key = props.getProperty("gpr.key")?.takeIf { it.isNotBlank() }

    if (user == null || key == null) {
        throw GradleException("'gpr.user' and 'gpr.key' must be set in 'signing.properties'")
    }

    return GprCredentials(user, key)
}

val gprCredentials by lazy {
    val credentials = loadGprCredentials()
    if (credentials == null && !gradle.startParameter.isOffline) {
        throw GradleException(
            "Missing GitHub credentials. Please either:\n" +
                "  1. Set GIT_ACTOR and GIT_TOKEN environment variables, or\n" +
                "  2. Create 'signing.properties' with 'gpr.user' and 'gpr.key'"
        )
    }
    credentials
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
        maven("https://maven.pkg.github.com/ReChronoRain/HyperCeiler") {
            credentials {
                username = gprCredentials?.user ?: "offline"
                password = gprCredentials?.key ?: "offline"
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
