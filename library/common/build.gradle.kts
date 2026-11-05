import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.library)
}

val statusBarFollowBuildAvailable = providers.gradleProperty("statusBarFollow").map(String::toBoolean).getOrElse(false)
val statusBarFollowServerBuildAvailable = providers.gradleProperty("statusBarFollowServer").map(String::toBoolean).getOrElse(false)

android {
    namespace = "com.sevtinge.hyperceiler.common"
    compileSdk = 37

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
        buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", "false")
        buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", "true")
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        release {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        create("beta") {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
        create("canary") {
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_AVAILABLE", statusBarFollowBuildAvailable.toString())
            buildConfigField("boolean", "STATUS_BAR_FOLLOW_SERVER_AVAILABLE", statusBarFollowServerBuildAvailable.toString())
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    api(libs.bundles.miuix)
    // libxposed API 102
    compileOnlyApi(libs.libxposed.api)
}
