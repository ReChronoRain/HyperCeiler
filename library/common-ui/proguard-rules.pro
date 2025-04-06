# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class androidx.preference.**{ *; }

-keep class com.sevtinge.hyperceiler.ui.app.holiday.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.dashboard.base.fragment.BasePreferenceFragment


-keep class com.sevtinge.hyperceiler.provision.activity.** { *; }
-keep class com.sevtinge.hyperceiler.provision.fragment.** { *; }

-keep class fan.**{ *; }
-keep class miuix.mgl.** { *; }

-dontwarn miui.util.HapticFeedbackUtil
-dontwarn com.android.internal.view.menu.MenuBuilder
