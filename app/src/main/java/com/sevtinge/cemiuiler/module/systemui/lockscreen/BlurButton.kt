package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object BlurButton : BaseHook() {
    override fun init() {

        //from com.sevtinge.cemiuiler.module.systemui.lockscreen.AddBlurEffectToLockScreen

        val keyguardBottomAreaViewClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView"
        ) ?: return

        XposedBridge.hookAllMethods(
            keyguardBottomAreaViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    /*if (!isDefaultLockScreenTheme()) {
                        return
                    }*/
                    val mLeftAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mLeftAffordanceView"
                    ) as ImageView
                    val mRightAffordanceView = HookUtils.getValueByField(
                        param.thisObject,
                        "mRightAffordanceView"
                    ) as ImageView

                    val keyguardBottomAreaView = param.thisObject as View
                    val leftBlurDrawable = HookUtils.createBlurDrawable(
                        keyguardBottomAreaView,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
                    val rightBlurDrawable = HookUtils.createBlurDrawable(
                        keyguardBottomAreaView,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val rightLayerDrawable = LayerDrawable(arrayOf(rightBlurDrawable))
                    leftLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    rightLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    mLeftAffordanceView.background = leftLayerDrawable
                    mRightAffordanceView.background = rightLayerDrawable
                }
            })
    }
}