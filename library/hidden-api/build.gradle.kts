plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.internal"
    compileSdk = 36

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("beta") {
            isMinifyEnabled = false
        }
        create("canary") {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
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
    implementation(libs.annotation)
}
