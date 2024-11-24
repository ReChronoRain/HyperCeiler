// file:noinspection DependencyNotationArgument
import com.android.build.gradle.internal.api.*
import com.android.build.gradle.tasks.*
import java.io.*
import java.text.*
import java.time.*
import java.time.format.*
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsparanoid)
    // alias(libs.plugins.lspluginResopt)
}

lsparanoid {
    seed = 227263
    classFilter = { true }
    includeDependencies = true
    variantFilter = { variant ->
        variant.buildType != "debug"
    }
}

val apkId = "HyperCeiler"
val buildTypes = "release"
val roots = mapOf(
    "animation" to "libs/animation-${buildTypes}.aar",
    "appcompat" to "libs/appcompat-${buildTypes}.aar",
    "basewidget" to "libs/basewidget-${buildTypes}.aar",
    "bottomsheet" to "libs/bottomsheet-${buildTypes}.aar",
    "cardview" to "libs/cardview-${buildTypes}.aar",
    "core" to "libs/core-${buildTypes}.aar",
    "flexible" to "libs/flexible-${buildTypes}.aar",
    "folme" to "libs/folme-${buildTypes}.aar",
    "graphics" to "libs/graphics-${buildTypes}.aar",
    "haptic" to "libs/haptic-${buildTypes}.aar",
    "navigator" to "libs/navigator-${buildTypes}.aar",
    "nestedheader" to "libs/nestedheader-${buildTypes}.aar",
    "pickerwidget" to "libs/pickerwidget-${buildTypes}.aar",
    "popupwidget" to "libs/popupwidget-${buildTypes}.aar",
    "preference" to "libs/preference-${buildTypes}.aar",
    "recyclerview" to "libs/recyclerview-${buildTypes}.aar",
    "smooth" to "libs/smooth-${buildTypes}.aar",
    "springback" to "libs/springback-${buildTypes}.aar",
    "stretchablewidget" to "libs/stretchablewidget-${buildTypes}.aar",
    "theme" to "libs/theme-${buildTypes}.aar",
    "viewpager" to "libs/viewpager-${buildTypes}.aar",
    "external" to "libs/external-${buildTypes}.aar",
    "expansion_packs" to "libs/hyperceiler_expansion_packs-debug.aar"
)

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
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = namespace
        minSdk = 33
        targetSdk = 35
        versionCode = 153
        versionName = "2.5.153"

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")

        ndk {
            // noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        additionalParameters += "--allow-reserved-package-id"
        additionalParameters += "--package-id"
        additionalParameters += "0x36"
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "/*.txt"
            excludes += "/*.bin"
            excludes += "/*.json"
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
        create("withoutProperties") {
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
            versionNameSuffix =
                "_${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())}"
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            signingConfig = if (properties != null) {
                signingConfigs["hasProperties"]
            } else {
                signingConfigs["withoutProperties"]
            }
        }
        create("beta") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            versionNameSuffix =
                "_${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())}"
            buildConfigField("String", "GIT_HASH", "\"${getGitHashLong()}\"")
            buildConfigField("String", "GIT_CODE", "\"$gitCode\"")
            signingConfig = if (properties != null) {
                signingConfigs["hasProperties"]
            } else {
                signingConfigs["withoutProperties"]
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
                signingConfigs["withoutProperties"]
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

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    kotlin.jvmToolchain(21)

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }
}

dependencies {
    compileOnly(project(":hidden-api"))
    compileOnly(libs.xposed.api)
    // compileOnly(libs.androidx.preference)

    implementation(libs.dexkit)
    implementation(libs.ezxhelper)
    implementation(libs.hiddenapibypass)
    implementation(libs.gson)
    implementation(libs.hooktool)
    implementation(libs.gson)
    implementation(libs.lyric.getter.api)

    implementation(libs.core)
    implementation(libs.collection)
    implementation(libs.recyclerview)
    implementation(libs.fragment)
    implementation(libs.lifecycle.common)
    implementation(libs.vectordrawable)
    implementation(libs.vectordrawable.animated)
    implementation(libs.customview)
    implementation(libs.customview.poolingcontainer)
    implementation(libs.coordinatorlayout)
    implementation(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    implementation(files(roots["animation"]))
    implementation(files(roots["appcompat"]))
    implementation(files(roots["basewidget"]))
    implementation(files(roots["bottomsheet"]))
    implementation(files(roots["cardview"]))
    implementation(files(roots["core"]))
    implementation(files(roots["flexible"]))
    implementation(files(roots["folme"]))
    implementation(files(roots["graphics"]))
    implementation(files(roots["haptic"]))
    implementation(files(roots["navigator"]))
    implementation(files(roots["nestedheader"]))
    implementation(files(roots["pickerwidget"]))
    implementation(files(roots["popupwidget"]))
    implementation(files(roots["preference"]))
    implementation(files(roots["recyclerview"]))
    implementation(files(roots["smooth"]))
    implementation(files(roots["springback"]))
    implementation(files(roots["stretchablewidget"]))
    implementation(files(roots["theme"]))
    implementation(files(roots["viewpager"]))
    implementation(files(roots["external"]))
    // project packs
    implementation(files(roots["expansion_packs"]))

    implementation(project(":app:processor"))
    annotationProcessor(project(":app:processor"))
}
