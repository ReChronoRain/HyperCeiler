-keep class com.sevtinge.hyperceiler.XposedInit
-keep class com.sevtinge.hyperceiler.module.skip.SystemFrameworkForCorePatch
-keep class com.sevtinge.hyperceiler.ui.LauncherActivity
-keep class com.sevtinge.hyperceiler.utils.blur.*
-keep class com.sevtinge.hyperceiler.utils.Helpers { boolean isModuleActive; }
-keep class com.sevtinge.hyperceiler.utils.Helpers { int XposedVersion; }
-keep class fan.**{*;}
-keep class org.luckypray.dexkit.*
-keep class * extends com.sevtinge.hyperceiler.ui.fragment.base.*
-keep class * extends com.sevtinge.hyperceiler.module.base.BaseHook { <init>(); }
-keep class * extends com.sevtinge.hyperceiler.module.base.BaseModule
-keep class com.sevtinge.hyperceiler.module.base.BaseModule {*;}
#-keep class com.sevtinge.hyperceiler.utils.XposedUtils {
#    *;
#}
-keep class com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.** {
    *;
}

-dontwarn android.app.ActivityTaskManager$RootTaskInfo
-dontwarn miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo
-dontwarn com.android.internal.view.menu.MenuBuilder
-allowaccessmodification
-overloadaggressively
