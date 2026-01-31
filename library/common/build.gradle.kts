plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.fan.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 35
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
    api(projects.library.core)
}
