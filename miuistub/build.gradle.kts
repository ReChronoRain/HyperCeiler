plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "miui.os"
    compileSdk = 35
}

dependencies {
    annotationProcessor(libs.annotation.processor)
    compileOnly(libs.refine.annotation)
    compileOnly(libs.androidx.preference)
}