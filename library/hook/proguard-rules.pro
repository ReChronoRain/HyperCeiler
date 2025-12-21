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
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Other ---
-keep class com.hchen.superlyricapi.** { *; }
-keep class org.luckypray.dexkit.** { *; }
-keep class org.lsposed.** { *; }
-keep class io.github.kyuubiran.ezxhelper.** { *; }

# --- Hook Tool ---
-keep class com.hchen.hooktool.** { *; }
-keep class * extends com.hchen.hooktool.HCBase { *; }

# --- HyperCeiler Core ---
-keep class com.sevtinge.hyperceiler.hook.XposedInit { *; }

-keep class com.sevtinge.hyperceiler.hook.module.** { *; }
-keep class com.sevtinge.hyperceiler.hook.safe.** { *; }

-keep class * extends com.sevtinge.hyperceiler.hook.module.base.BaseModule { *; }
-keep class * extends com.sevtinge.hyperceiler.hook.module.base.BaseHook { <init>(...); }

# --- Tool ---
-keep class com.sevtinge.hyperceiler.hook.utils.blur.** { *; }
-keep class com.sevtinge.hyperceiler.hook.utils.api.effect.** { *; }
-keep class com.sevtinge.hyperceiler.hook.utils.api.miuiStringToast.res.** { *; }
-keep class com.sevtinge.hyperceiler.hook.utils.input.ContentModel { *; }
-keep class com.sevtinge.hyperceiler.hook.utils.input.FileHelper { *; }
