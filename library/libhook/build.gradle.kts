plugins {
    alias(libs.plugins.android.library)
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
    namespace = "com.sevtinge.hyperceiler.libhook"
    compileSdk = 37

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+MultiDollarInterpolation")
    }
}

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
    compileOnlyApi(projects.library.hiddenApi)

    // libxposed API 102 (compileOnly, runs against framework on device)
    compileOnlyApi(libs.libxposed.api)
    api(libs.libxposed.service)

    api(libs.dexkit)
    api(libs.ezhooktool.core)
    api(libs.ezhooktool.xposed102)
    api(libs.hiddenapibypass)
    api(libs.gson)
    api(libs.hyperfocusapi)
    api(libs.superlyricapi)
    api(libs.lunarcalendar)

    api(projects.library.processor)
    api(projects.library.common)
    annotationProcessor(projects.library.processor)
}
