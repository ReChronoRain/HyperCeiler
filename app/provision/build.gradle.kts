plugins {
    alias(libs.plugins.android.library)
}

val buildTypes = "debug"
val roots = mapOf(
    "animation" to "libs/animation-${buildTypes}.aar",
    "appcompat" to "libs/appcompat-${buildTypes}.aar",
    "basewidget" to "libs/basewidget-${buildTypes}.aar",
    "bottomsheet" to "libs/bottomsheet-${buildTypes}.aar",
    "cardview" to "libs/cardview-${buildTypes}.aar",
    "core" to "libs/core-${buildTypes}.aar",
    "flexible" to "libs/flexible-${buildTypes}.aar",
    "folme" to "libs/folme-${buildTypes}.aar",
    "graphics" to "libs/graphics-${buildTypes}.aar",
    "haptic" to "libs/haptic-${buildTypes}.aar",
    "navigator" to "libs/navigator-${buildTypes}.aar",
    "nestedheader" to "libs/nestedheader-${buildTypes}.aar",
    "pickerwidget" to "libs/pickerwidget-${buildTypes}.aar",
    "popupwidget" to "libs/popupwidget-${buildTypes}.aar",
    "preference" to "libs/preference-${buildTypes}.aar",
    "recyclerview" to "libs/recyclerview-${buildTypes}.aar",
    "smooth" to "libs/smooth-${buildTypes}.aar",
    "springback" to "libs/springback-${buildTypes}.aar",
    "slidingwidget" to "libs/slidingwidget-${buildTypes}.aar",
    "stretchablewidget" to "libs/stretchablewidget-${buildTypes}.aar",
    "theme" to "libs/theme-${buildTypes}.aar",
    "viewpager" to "libs/viewpager-${buildTypes}.aar",
    "external" to "libs/external-${buildTypes}.aar",
    "expansion_packs" to "libs/hyperceiler_expansion_packs-debug.aar"
)

android {
    namespace = "com.sevtinge.provision"
    compileSdk = 35

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}

dependencies {
    api(libs.core)
    api(libs.collection)
    api(libs.recyclerview)
    api(libs.fragment)
    api(libs.lifecycle.common)
    api(libs.coordinatorlayout)
    api(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    api(files(roots["animation"]))
    api(files(roots["appcompat"]))
    api(files(roots["basewidget"]))
    api(files(roots["bottomsheet"]))
    api(files(roots["cardview"]))
    api(files(roots["core"]))
    api(files(roots["flexible"]))
    api(files(roots["folme"]))
    api(files(roots["graphics"]))
    api(files(roots["haptic"]))
    api(files(roots["navigator"]))
    api(files(roots["nestedheader"]))
    api(files(roots["pickerwidget"]))
    api(files(roots["popupwidget"]))
    api(files(roots["preference"]))
    api(files(roots["recyclerview"]))
    api(files(roots["smooth"]))
    api(files(roots["springback"]))
    api(files(roots["slidingwidget"]))
    api(files(roots["stretchablewidget"]))
    api(files(roots["theme"]))
    api(files(roots["viewpager"]))
    api(files(roots["external"]))
    // project packs
    api(files(roots["expansion_packs"]))
}