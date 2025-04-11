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

-keep class com.sevtinge.hyperceiler.hook.XposedInit { *; }
-keep class com.sevtinge.hyperceiler.hook.module.skip.SystemFrameworkForCorePatch { *; }

-keep class com.sevtinge.hyperceiler.hook.module.**{ *; }

-keep class * extends com.sevtinge.hyperceiler.hook.module.base.BaseHook { <init>(); }

-keep class com.sevtinge.hyperceiler.hook.module.base.dexkit.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.hook.module.base.BaseModule
-keep class com.sevtinge.hyperceiler.hook.module.base.BaseModule { *; }

-keep class com.sevtinge.hyperceiler.hook.utils.blur.*
-keep class com.sevtinge.hyperceiler.hook.utils.api.miuiStringToast.res.** { *; }
-keep class com.sevtinge.hyperceiler.hook.utils.ContentModel {*;}
-keep class com.sevtinge.hyperceiler.hook.utils.FileHelper {*;}

-keep class com.github.kyuubiran.ezxhelper.** { *; }
-keep class com.hchen.hooktool.** { *; }

-dontwarn de.robv.android.xposed.**
-dontwarn miui.**
-dontwarn android.app.AndroidAppHelper
-dontwarn android.content.res.**

-dontwarn android.app.ActivityTaskManager$RootTaskInfo
-dontwarn miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.annotation.processing.SupportedOptions
-dontwarn javax.annotation.processing.SupportedSourceVersion
-dontwarn javax.annotation.processing.Processor

-allowaccessmodification
