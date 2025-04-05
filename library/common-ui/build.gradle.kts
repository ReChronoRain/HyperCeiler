plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 34
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("beta") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("canary") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {

    api(libs.miuix.animation)
    api(libs.miuix.appcompat)
    api(libs.miuix.basewidget)
    api(libs.miuix.bottomsheet)
    api(libs.miuix.cardview)
    api(libs.miuix.core)
    api(libs.miuix.flexible)
    api(libs.miuix.folme)
    api(libs.miuix.graphics)
    api(libs.miuix.haptic)
    api(libs.miuix.mgl)
    api(libs.miuix.navigator)
    api(libs.miuix.nestedheader)
    api(libs.miuix.pickerwidget)
    api(libs.miuix.popupwidget)
    api(libs.miuix.preference)
    api(libs.miuix.recyclerview)
    api(libs.miuix.slidingwidget)
    api(libs.miuix.smooth)
    api(libs.miuix.springback)
    api(libs.miuix.stretchablewidget)
    api(libs.miuix.theme)
    api(libs.miuix.viewpager)
    api(libs.miuix.visualcheck)

    api(projects.library.hook)
}
