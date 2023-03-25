package com.sevtinge.cemiuiler.module.home.dock

import android.view.View
import android.view.ViewGroup
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod
import com.sevtinge.cemiuiler.utils.getObjectField
import com.sevtinge.cemiuiler.utils.hookAfterMethod

object HideSeekPoint : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_hide_seek_point")) return
        "com.miui.home.launcher.ScreenView".hookAfterMethod(
            "updateSeekPoints", Int::class.javaPrimitiveType
        ) {
            showSeekBar(it.thisObject as View)
        }
        "com.miui.home.launcher.ScreenView".hookAfterMethod(
            "addView", View::class.java, Int::class.javaPrimitiveType, ViewGroup.LayoutParams::class.java
        ) {
            showSeekBar(it.thisObject as View)
        }
        "com.miui.home.launcher.ScreenView".hookAfterMethod(
            "removeScreen", Int::class.javaPrimitiveType
        ) {
            showSeekBar(it.thisObject as View)
        }
        "com.miui.home.launcher.ScreenView".hookAfterMethod(
            "removeScreensInLayout", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        ) {
            showSeekBar(it.thisObject as View)
        }
    }

    private fun showSeekBar(view: View) {
        if ("Workspace" != view.javaClass.simpleName) return
        val mScreenSeekBar = view.getObjectField("mScreenSeekBar") as View
        val isInEditingMode = view.callMethod("isInNormalEditingMode") as Boolean
        if (!isInEditingMode) {
            mScreenSeekBar.animate().cancel()
            mScreenSeekBar.visibility = View.GONE
        }

    }
}