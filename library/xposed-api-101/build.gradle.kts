plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.kyuubiran.ezxhelper.xposed"
    compileSdk = 36

    defaultConfig {
        minSdk = 35
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {}
        create("beta")
        create("canary")
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
}

dependencies {
    compileOnlyApi(libs.libxposed.api)
    implementation(libs.annotation)
    implementation(libs.ezxhelper.core)
}
