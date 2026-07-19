plugins {
    alias(libs.plugins.android.library)
}
android {

    namespace = "com.sevtinge.hyperceiler.provision"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    buildFeatures {
        aidl = true
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

dependencies {
    api(projects.library.common)
    api(projects.library.libhook)
}
