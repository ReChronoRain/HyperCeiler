package com.sevtinge.hyperceiler.module.hook.home.dock

import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.FrameLayout
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.dp2px
import com.sevtinge.hyperceiler.utils.blur.MiBlurViewKt
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod

@SuppressLint("StaticFieldLeak")
object DockCustomNew : BaseHook() {
    override fun init() {
        val launcherClass = "com.miui.home.launcher.Launcher".findClass()

        launcherClass.hookAfterMethod("setupViews") {
            val mHotSeats = it.thisObject.getObjectField("mHotSeats") as FrameLayout
            val mDockBlurParent = FrameLayout(appContext)
            val mDockBlur = MiBlurViewKt(appContext, mPrefsMap.getInt("home_dock_bg_radius", 30))
            val mDockHeight = dp2px(appContext, mPrefsMap.getInt("home_dock_bg_height", 80).toFloat())
            val mDockMargin = dp2px(appContext, (mPrefsMap.getInt("home_dock_bg_margin_horizontal", 30) - 6).toFloat())
            val mDockBottomMargin = dp2px(appContext, (mPrefsMap.getInt("home_dock_bg_margin_bottom", 30) - 92).toFloat())
            mDockBlurParent.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mDockHeight).also { layoutParams ->
                layoutParams.gravity = Gravity.BOTTOM
                layoutParams.setMargins(mDockMargin, 0, mDockMargin, mDockBottomMargin)
            }
            mDockBlur.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            mHotSeats.addView(mDockBlurParent, 0)
            mDockBlurParent.addView(mDockBlur, 0)
        }
    }
}
