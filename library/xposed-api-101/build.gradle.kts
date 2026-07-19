plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.lingqiqi5211.ezhooktool.xposed"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    buildTypes {
        release {}
        create("beta")
        create("canary")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    compileOnlyApi(libs.libxposed.api)
    implementation(libs.annotation)
    api(libs.ezhooktool.core)
}
