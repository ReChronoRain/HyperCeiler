plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 35

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
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

dependencies {
    api(projects.library.hook)
    api(projects.library.provision)
}
