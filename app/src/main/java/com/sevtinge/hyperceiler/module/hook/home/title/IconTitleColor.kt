package com.sevtinge.hyperceiler.module.hook.home.title

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.Log
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

object IconTitleColor : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {

        val value = mPrefsMap.getInt("home_title_title_color", -1)
        val launcherClass = "com.miui.home.launcher.Launcher".findClass()
        val shortcutInfoClass = "com.miui.home.launcher.ShortcutInfo".findClass()
        if (value == -1) return
        try {
            "com.miui.home.launcher.ItemIcon".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.maml.MaMlWidgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.LauncherMtzGadgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.LauncherWidgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.ShortcutIcon".hookAfterMethod(
                "fromXml", Int::class.javaPrimitiveType, launcherClass, ViewGroup::class.java, shortcutInfoClass
            ) {
                val buddyIconView = it.args[3].callMethod("getBuddyIconView", it.args[2]) as View
                val mTitle = buddyIconView.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.ShortcutIcon".hookAfterMethod(
                "createShortcutIcon", Int::class.javaPrimitiveType, launcherClass, ViewGroup::class.java
            ) {
                val buddyIcon = it.result as View
                val mTitle = buddyIcon.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.common.Utilities".hookAfterMethod(
                "adaptTitleStyleToWallpaper",
                Context::class.java,
                TextView::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ) {
                val mTitle = it.args[1] as TextView
                if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextColor(value)
                }
            }
        } catch (e: Throwable) {
            Log.ex(e)
        }

    }
}
