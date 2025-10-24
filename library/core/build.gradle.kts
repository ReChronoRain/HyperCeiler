plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 34

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
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

dependencies {
    api(projects.library.hook)
    api(projects.library.provision)
}
