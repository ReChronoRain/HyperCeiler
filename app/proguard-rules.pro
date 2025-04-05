
-keep class com.sevtinge.hyperceiler.ui.LauncherActivity

-keep class com.sevtinge.hyperceiler.ui.app.holiday.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.ui.base.BasePreferenceFragment

-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { boolean isModuleActive; }
-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { int XposedVersion; }

-keep class androidx.preference.**{ *; }
-keep class com.sevtinge.provision.activity.** { *; }
-keep class com.sevtinge.provision.fragment.** { *; }

-keep class fan.**{ *; }
-keep class miuix.mgl.** { *; }

-keep class cn.lyric.getter.api.**{ *; }
-keep class org.luckypray.dexkit.**{ *; }

-keepattributes SourceFile,LineNumberTable
-dontwarn android.app.ActivityTaskManager$RootTaskInfo
-dontwarn miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo
-dontwarn com.android.internal.view.menu.MenuBuilder
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.annotation.processing.SupportedOptions
-dontwarn javax.annotation.processing.SupportedSourceVersion
-dontwarn javax.annotation.processing.Processor
-dontwarn miui.util.HapticFeedbackUtil
-allowaccessmodification
-obfuscationdictionary          dict.txt
-classobfuscationdictionary     dict.txt
-packageobfuscationdictionary   dict.txt

