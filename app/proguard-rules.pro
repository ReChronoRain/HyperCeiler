
-keep class com.sevtinge.hyperceiler.ui.LauncherActivity

-keep class com.sevtinge.hyperceiler.ui.app.holiday.**{ *; }
-keep class * extends com.sevtinge.hyperceiler.ui.base.BasePreferenceFragment

-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { boolean isModuleActive; }
-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { int XposedVersion; }

-keep class cn.lyric.getter.api.**{ *; }
-keep class org.luckypray.dexkit.**{ *; }

-keepattributes SourceFile,LineNumberTable
-dontwarn android.app.ActivityTaskManager$RootTaskInfo
-dontwarn miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.annotation.processing.SupportedOptions
-dontwarn javax.annotation.processing.SupportedSourceVersion
-dontwarn javax.annotation.processing.Processor
-allowaccessmodification
-obfuscationdictionary          dict.txt
-classobfuscationdictionary     dict.txt
-packageobfuscationdictionary   dict.txt

