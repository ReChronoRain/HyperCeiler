// file:noinspection DependencyNotationArgument
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
}

val apkId = "HyperCeiler"
val gitHash: String by lazy { runGitCommand("rev-parse", "--short", "HEAD") ?: "unknown" }
val gitHashLong: String by lazy { runGitCommand("rev-parse", "HEAD") ?: "unknown" }
val gitCommitCount: Int by lazy { runGitCommand("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 0 }
val gitBranch: String by lazy {
    val url = runGitCommand("remote", "get-url", "origin") ?: "unknown"
    val branch = runGitCommand("branch", "--show-current") ?: "unknown"
    """github\.com[:/](.+?)(\.git)?$""".toRegex().find(url)?.groupValues?.get(1).orEmpty() + "/" + branch
}
val gitVersionCode: Int by lazy { 5 + gitCommitCount }

fun runGitCommand(vararg args: String): String? = runCatching {
    ProcessBuilder(listOf("git") + args)
        .redirectErrorStream(true)
        .start()
        .let { process ->
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }
}.getOrNull()

fun loadPropertiesFromFile(fileName: String): Properties? =
    rootProject.file(fileName).takeIf { it.exists() }?.let { file ->
        Properties().apply { load(file.inputStream()) }
    }

android {
    namespace = "com.sevtinge.hyperceiler"
    compileSdk = 36
    compileSdkMinor = 1
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = namespace
        minSdk = 35
        targetSdk = 36
        versionCode = gitVersionCode
        versionName = "2.10.165"

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())

        val buildConfigData = mapOf(
            "BUILD_TIME" to buildTime,
            "BUILD_OS_NAME" to System.getProperty("os.name"),
            "BUILD_USER_NAME" to System.getProperty("user.name"),
            "BUILD_JAVA_VERSION" to System.getProperty("java.version"),
            "GIT_BRANCH" to gitBranch
        )

        for ((key, value) in buildConfigData) {
            buildConfigField("String", key, "\"$value\"")
        }

        ndk {
            // noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x36")
    }

    packaging {
        resources {
            merges += listOf("META-INF/xposed/*")
            excludes += listOf("**")
        }
        dex {
            useLegacyPackaging = true
        }
    }

    val properties: Properties? = loadPropertiesFromFile("signing.properties")
    fun getString(propertyName: String, environmentName: String, prompt: String): String =
        properties?.getProperty(propertyName)
            ?: System.getenv(environmentName)
            ?: System.console()?.readLine("\n$prompt: ").orEmpty()

    val buildTimeSuffix: String by lazy {
        SimpleDateFormat("MMddHHmm").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())
    }
    val dateSuffix: String by lazy {
        DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())
    }

    signingConfigs {
        create("hasProperties") {
            if (properties != null) {
                storeFile = file(getString("storeFile", "STORE_FILE", "Store file"))
                storePassword = getString("storePassword", "STORE_PASSWORD", "Store password")
                keyAlias = getString("keyAlias", "KEY_ALIAS", "Key alias")
                keyPassword = getString("keyPassword", "KEY_PASSWORD", "Key password")
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        val configSigning: ApplicationBuildType.() -> Unit = {
            val signingConfigName = if (properties != null) "hasProperties" else "debug"
            signingConfig = signingConfigs.findByName(signingConfigName)
        }

        val applyBase: ApplicationBuildType.() -> Unit = {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
        }

        release {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            proguardFiles("proguard-log.pro")
            versionNameSuffix = "-$dateSuffix"
        }

        create("beta") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-$dateSuffix"
        }

        create("canary") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-${gitHash}-r${gitVersionCode}"
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
            versionNameSuffix = "-${buildTimeSuffix}-r${gitVersionCode}"
            if (properties != null) {
                signingConfig = signingConfigs.findByName("hasProperties")
            }
        }
    }

}

afterEvaluate {
    base {
        val buildTypeName = gradle.startParameter.taskNames
            .firstOrNull { it.contains("assemble", ignoreCase = true) }
            ?.substringAfterLast(":")
            ?.replace("assemble", "", ignoreCase = true)
            ?.lowercase() ?: "debug"
        val suffix = android.buildTypes.findByName(buildTypeName)?.versionNameSuffix ?: ""
        archivesName.set("$apkId-${android.defaultConfig.versionName}$suffix")
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin.jvmToolchain(21)

dependencies {
    implementation(libs.expansion)
    implementation(projects.library.core)
    implementation(projects.library.common)
}
