import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "fan.miuix"
    compileSdk = 35

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
        }
    }
}

publishing {
    publications {
        val directory = File("libs")
        val files = directory.listFiles()?.filter { it.isFile } ?: emptyList()

        for (i in files) {
            val groupName = i.name.substring(startIndex = 0, endIndex = i.name.length - 10)

            create<MavenPublication>(groupName) {
                groupId = "fan"
                artifactId = groupName
                version = "3.0"
                artifact("../libs/$groupName-debug.aar")
            }
        }
    }
}

afterEvaluate {
    tasks.clean.dependsOn("publishToMavenLocal")
    tasks.preBuild.dependsOn("publishToMavenLocal")
}