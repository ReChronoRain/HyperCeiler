plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    id("org.lsposed.lsparanoid") version "0.6.0" apply false
    // id("org.lsposed.lsplugin.resopt") version "1.6" apply false
}
