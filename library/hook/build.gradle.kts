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
    namespace = "com.sevtinge.hyperceiler.hook"
    compileSdk = 36

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        release {
            consumerProguardFiles("proguard-rules.pro")
        }
        create("beta") {
            consumerProguardFiles("proguard-rules.pro")
        }
        create("canary") {
            consumerProguardFiles("proguard-rules.pro")
        }
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
    compileOnlyApi(libs.xposed.api)

    api(libs.dexkit)
    api(libs.bundles.ezxhelper)
    api(libs.hiddenapibypass)
    api(libs.gson)
    api(libs.hooktool)
    api(libs.hyperfocusapi)
    api(libs.superlyricapi)
    api(libs.lunarcalendar)

    api(projects.library.processor)
    annotationProcessor(projects.library.processor)
}
