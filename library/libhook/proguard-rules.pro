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

# --- Xposed ---
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onPackageLoaded(...);
    public void onSystemServerLoaded(...);
}
-keep,allowoptimization,allowobfuscation @io.github.libxposed.api.annotations.* class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}

# --- Other ---
-keep class com.hchen.superlyricapi.** { *; }
-keep class org.luckypray.dexkit.** { *; }
-keep class org.lsposed.** { *; }
-keep class io.github.kyuubiran.ezxhelper.** { *; }

# --- HyperCeiler Core ---
-keep class com.sevtinge.hyperceiler.libhook.app.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.rules.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.safecrash.** { *; }
-keep class * extends com.sevtinge.hyperceiler.libhook.base.BaseHook { <init>(...); }

# --- Tool ---
-keep class com.sevtinge.hyperceiler.libhook.utils.hookapi.miuiStringToast.res.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.** { *; }
-keep class com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.** { *; }
