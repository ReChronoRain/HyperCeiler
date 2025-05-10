// file:noinspection DependencyNotationArgument
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val apkId = "HyperCeiler"

val getGitCommitCount: () -> Int = {
    val output = ByteArrayOutputStream()
    ProcessBuilder("git", "rev-list", "--count", "HEAD").start().apply {
        inputStream.copyTo(output)
        waitFor()
    }
    output.toString().trim().toInt()
}

val getVersionCode: () -> Int = {
    val commitCount = getGitCommitCount()
    val major = 5
    major + commitCount
}

fun getGitHash(): String {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    return output
}

fun getGitHashLong(): String {
    val process = ProcessBuilder("git", "rev-parse", "HEAD").start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    return output
}

fun loadPropertiesFromFile(fileName: String): Properties? {
    val propertiesFile = rootProject.file(fileName)
    return if (propertiesFile.exists()) {
        val properties = Properties()
        properties.load(propertiesFile.inputStream())
        properties
    } else null
}

android {
    namespace = "com.sevtinge.hyperceiler"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = namespace
        minSdk = 34
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = "2.6.160"

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        val buildTime = sdf.format(Date())
        val osName = System.getProperty("os.name")
        // val osArch = System.getProperty("os.arch")
        val userName = System.getProperty("user.name")
        val javaVersion = System.getProperty("java.version")
        // val javaVendor = System.getProperty("java.vendor") + " (" + System.getProperty("java.vendor.url") + ")"

        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_OS_NAME", "\"$osName\"")
        // buildConfigField("String", "BUILD_OS_ARCH", "\"$osArch\"")
        buildConfigField("String", "BUILD_USER_NAME", "\"$userName\"")
        buildConfigField("String", "BUILD_JAVA_VERSION", "\"$javaVersion\"")
        // buildConfigField("String", "BUILD_JAVA_VENDOR", "\"$javaVendor\"")

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
            excludes += listOf("/META-INF/**", "/kotlin/**", "/*.txt", "/*.bin", "/*.json")
        }
        dex {
            useLegacyPackaging = true
        }
        applicationVariants.all {
            outputs.all {
                (this as BaseVariantOutputImpl).outputFileName =
                    "${apkId}_${versionName}_${versionCode}_${buildType.name}.apk"
            }
        }
    }

    val properties: Properties? = loadPropertiesFromFile("signing.properties")
    val getString: (String, String, String) -> String = { propertyName, environmentName, prompt ->
        properties?.getProperty(propertyName)
            ?: System.getenv(environmentName)
            ?: System.console()?.readLine("\n$prompt: ") ?: ""
    }
    val gitCode = getVersionCode()
    val gitHash = getGitHash()

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
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-log.pro"
            )
            versionNameSuffix = "_${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())}"
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            signingConfig = if (properties != null) {
                signingConfigs["hasProperties"]
            } else {
                signingConfigs["debug"]
            }
        }
        create("beta") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            versionNameSuffix = "_${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())}"
            buildConfigField("String", "GIT_HASH", "\"${getGitHashLong()}\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            signingConfig = if (properties != null) {
                signingConfigs["hasProperties"]
            } else {
                signingConfigs["debug"]
            }
        }
        create("canary") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            versionNameSuffix = "_${gitHash}_r${gitCode}"
            buildConfigField("String", "GIT_HASH", "\"${getGitHashLong()}\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            signingConfig = if (properties != null) {
                signingConfigs["hasProperties"]
            } else {
                signingConfigs["debug"]
            }
        }
        debug {
            versionNameSuffix = "_${gitHash}_r${gitCode}"
            buildConfigField("String", "GIT_HASH", "\"${getGitHashLong()}\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            if (properties != null) {
                signingConfig = signingConfigs["hasProperties"]
            }
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
    implementation(libs.expansion)
    implementation(projects.library.commonUi)
}
