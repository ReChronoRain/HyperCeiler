plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsparanoid)
}

lsparanoid {
    seed = 227263
    classFilter = { true }
    includeDependencies = true
    variantFilter = { variant ->
        variant.buildType != "debug"
    }
}

android {
    namespace = "com.sevtinge.hyperceiler.hook"
    compileSdk = 36

    defaultConfig {
        minSdk = 34

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        release {
            consumerProguardFiles(libs.versions.proguard.rules.get())
        }
        create("beta") {
            consumerProguardFiles(libs.versions.proguard.rules.get())
        }
        create("canary") {
            consumerProguardFiles(libs.versions.proguard.rules.get())
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin.jvmToolchain(21)

dependencies {
    api(libs.core)
    api(libs.fragment)
    api(libs.recyclerview)
    api(libs.coordinatorlayout)
    api(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    api(libs.core.ktx)
    api(libs.expansion)
    compileOnly(projects.library.hiddenApi)
    compileOnly(libs.xposed.api)

    api(libs.dexkit)
    api(libs.mmkv)
    api(libs.ezxhelper)
    api(libs.hiddenapibypass)
    api(libs.gson)
    api(libs.hooktool)
    api(libs.hyperfocusapi)
    api(libs.superlyricapi)
    api(libs.lunarcalendar)

    api(projects.library.processor)
    annotationProcessor(projects.library.processor)
}
