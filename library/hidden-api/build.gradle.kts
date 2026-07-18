plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.internal"
    compileSdk = 37

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
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin.jvmToolchain(25)

dependencies {
    implementation(libs.annotation)
}
