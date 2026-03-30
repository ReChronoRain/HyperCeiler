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
    compileSdk = 37
    compileSdkMinor = 0
    buildToolsVersion = "37.0.0"

    val propGitHash = "GIT_HASH"
    val propGitCode = "GIT_CODE"
    val scHasProperties = "hasProperties"
    val scDebug = "debug"
    val typeString = "String"

    val buildTimeSuffix = SimpleDateFormat("MMddHHmm").apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(Date())
    val dateSuffixString = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())

    // Quoted values for buildConfigField to satisfy DRY checks
    val valGitHash = "\"$gitHash\""
    val valGitHashLong = "\"$gitHashLong\""
    val valGitCode = "\"$gitVersionCode\""

    // Repeated suffix strings
    val suffixDate = "-$dateSuffixString"
    val suffixVersion = "-r${gitVersionCode}"

    defaultConfig {
        applicationId = namespace
        minSdk = 35
        targetSdk = 37
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
            buildConfigField(typeString, key, "\"$value\"")
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
    val hasSigning = properties?.containsKey("storeFile") == true || System.getenv("STORE_FILE") != null

    fun getString(propertyName: String, environmentName: String, prompt: String): String =
        properties?.getProperty(propertyName)
            ?: System.getenv(environmentName)
            ?: System.console()?.readLine("\n$prompt: ").orEmpty()

    signingConfigs {
        create(scHasProperties) {
            if (hasSigning) {
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
            val signingConfigName = if (hasSigning) scHasProperties else scDebug
            signingConfig = signingConfigs.findByName(signingConfigName)
        }

        val applyBase: ApplicationBuildType.() -> Unit = {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField(typeString, propGitCode, valGitCode)
        }

        release {
            applyBase()
            configSigning()
            buildConfigField(typeString, propGitHash, valGitHash)
            proguardFiles("proguard-log.pro")
            versionNameSuffix = suffixDate
        }

        create("beta") {
            applyBase()
            configSigning()
            buildConfigField(typeString, propGitHash, valGitHashLong)
            versionNameSuffix = suffixDate
        }

        create("canary") {
            applyBase()
            configSigning()
            buildConfigField(typeString, propGitHash, valGitHashLong)
            versionNameSuffix = "-${gitHash}${suffixVersion}"
        }

        debug {
            isMinifyEnabled = false
            buildConfigField(typeString, propGitHash, valGitHashLong)
            buildConfigField(typeString, propGitCode, valGitCode)
            versionNameSuffix = "-${buildTimeSuffix}${suffixVersion}"
            if (hasSigning) {
                signingConfig = signingConfigs.findByName(scHasProperties)
            }
        }
    }

    afterEvaluate {
        base {
            val buildTypeName = gradle.startParameter.taskNames
                .firstOrNull { it.contains("assemble", ignoreCase = true) }
                ?.substringAfterLast(":")
                ?.replace("assemble", "", ignoreCase = true)
                ?.lowercase() ?: scDebug
            val suffix = android.buildTypes.findByName(buildTypeName)?.versionNameSuffix ?: ""
            archivesName.set("$apkId-${android.defaultConfig.versionName}$suffix")
        }
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
    implementation(libs.viewpager2)
    implementation(libs.expansion)
    implementation(projects.library.core)
    implementation(projects.library.common)

    api (libs.room.runtime)
    // FTS 支持
    api (libs.room.ktx)
    annotationProcessor (libs.room.compiler)
}
