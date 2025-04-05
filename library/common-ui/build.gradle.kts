plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler"
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
    implementation(libs.core)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.coordinatorlayout)
    implementation(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    implementation(libs.miuix.animation)
    implementation(libs.miuix.appcompat)
    implementation(libs.miuix.basewidget)
    implementation(libs.miuix.bottomsheet)
    implementation(libs.miuix.cardview)
    implementation(libs.miuix.core)
    implementation(libs.miuix.flexible)
    implementation(libs.miuix.folme)
    implementation(libs.miuix.graphics)
    implementation(libs.miuix.haptic)
    implementation(libs.miuix.mgl)
    implementation(libs.miuix.navigator)
    implementation(libs.miuix.nestedheader)
    implementation(libs.miuix.pickerwidget)
    implementation(libs.miuix.popupwidget)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.recyclerview)
    implementation(libs.miuix.slidingwidget)
    implementation(libs.miuix.smooth)
    implementation(libs.miuix.springback)
    implementation(libs.miuix.stretchablewidget)
    implementation(libs.miuix.theme)
    implementation(libs.miuix.viewpager)
    implementation(libs.miuix.visualcheck)
}
