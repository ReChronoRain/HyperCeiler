plugins {
    alias(libs.plugins.android.library)
}
android {

    namespace = "com.sevtinge.hyperceiler.provision"
    compileSdk = 36

    defaultConfig {
        minSdk = 34
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
    api(projects.library.common)
}
