plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.provision"
    compileSdk = 35

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

    implementation(projects.miuix)
    val directory = File("libs")
    val files = directory.listFiles()?.filter { it.isFile } ?: emptyList()
    files.forEach { i ->
        val groupName = i.name.substring(startIndex = 0, endIndex = i.name.length - 10)
        implementation("fan:$groupName:3.0")
    }
}
