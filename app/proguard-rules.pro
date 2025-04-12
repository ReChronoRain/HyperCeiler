-keep class com.sevtinge.hyperceiler.ui.**{ *; }

-keep class androidx.preference.**{ *; }
-keep class fan.**{ *; }
-keep class com.fan.**{ *; }

-keepattributes SourceFile,LineNumberTable

-dontwarn com.android.internal.view.menu.MenuBuilder
-dontwarn de.robv.android.xposed.IXposedHookZygoteInit$StartupParam
-dontwarn de.robv.android.xposed.XposedBridge
-dontwarn de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam
-dontwarn miui.util.HapticFeedbackUtil

-dontwarn android.app.ActivityTaskManager$RootTaskInfo
-dontwarn miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.annotation.processing.SupportedOptions
-dontwarn javax.annotation.processing.SupportedSourceVersion
-dontwarn javax.annotation.processing.Processor
-dontwarn android.view.ViewRootImpl
-dontwarn com.android.internal.graphics.drawable.BackgroundBlurDrawable

-allowaccessmodification
#-obfuscationdictionary          dict.txt
#-classobfuscationdictionary     dict.txt
#-packageobfuscationdictionary   dict.txt

