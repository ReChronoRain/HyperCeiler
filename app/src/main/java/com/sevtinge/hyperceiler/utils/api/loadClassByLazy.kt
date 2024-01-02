package com.sevtinge.hyperceiler.utils.api

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass

// by StarVoyager
object LazyClass {
    val clazzMiuiBuild by lazy {
        loadClass("miui.os.Build")
    }

    val AndroidBuildCls by lazy {
        loadClass("android.os.Build")
    }

    val SettingsFeaturesClass by lazy {
        loadClass("com.android.settings.utils.SettingsFeatures")
    }

    val FeatureParserCls by lazy {
        loadClass("miui.util.FeatureParser")
    }

    val SystemProperties by lazy {
        loadClass("android.os.SystemProperties")
    }

    val MiuiBuildCls by lazy {
        loadClass("miui.os.Build")
    }

    val SettingsFeaturesCls by lazy {
        loadClass("com.android.settings.utils.SettingsFeatures")
    }

    val AiasstVisionSystemUtilsCls by lazy {
        loadClass("com.xiaomi.aiasst.vision.utils.SystemUtils")
    }

    val SupportAiSubtitlesUtils by lazy {
        loadClass("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils")
    }

    val MiuiSettingsCls by lazy {
        loadClass("com.android.settings.MiuiSettings")
    }

    val ShellResourceFetcher by lazy {
        loadClass("com.miui.gallery.editor.photo.screen.shell.res.ShellResourceFetcher")
    }

    val mNewClockClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiStatusBarClock")
    }

    val StrongToast by lazy {
        loadClass("com.android.systemui.toast.MIUIStrongToast")
    }
}
