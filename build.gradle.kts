plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Keep Kotlin 2.4.10 on the built-in Kotlin classpath for EzHookTool 1.1.0.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.lsparanoid) apply false
}
