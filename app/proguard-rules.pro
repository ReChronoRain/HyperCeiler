-keep class com.sevtinge.hyperceiler.XposedInit
-keep class com.sevtinge.hyperceiler.module.skip.SystemFrameworkForCorePatch
-keep class com.sevtinge.hyperceiler.ui.LauncherActivity
-keep class com.sevtinge.hyperceiler.utils.blur.*
-keep class com.sevtinge.hyperceiler.module.base.tool.AppsTool { boolean isModuleActive; }
-keep class com.sevtinge.hyperceiler.module.base.tool.AppsTool { int XposedVersion; }
-keep class com.sevtinge.hyperceiler.module.base.**{ *; }
-keep class com.sevtinge.hyperceiler.ui.app.holiday.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.ui.base.BasePreferenceFragment
-keep class * extends com.sevtinge.hyperceiler.module.base.BaseHook { <init>(); }
-keep class com.sevtinge.hyperceiler.module.base.dexkit.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.module.base.BaseModule
-keep class com.sevtinge.hyperceiler.module.base.BaseModule { *; }
-keep class com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.** { *; }
-keep class com.sevtinge.hyperceiler.utils.ContentModel {*;}
-keep class com.sevtinge.hyperceiler.utils.FileHelper {*;}

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

