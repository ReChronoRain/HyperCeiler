package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageView
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils.createBlurDrawable
import com.sevtinge.cemiuiler.utils.HookUtils.getValueByField
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask

object BlurButton : BaseHook() {
    override fun init() {
        var mLeftAffordanceView: WeakReference<ImageView>? = null
        var mRightAffordanceView: WeakReference<ImageView>? = null
        var keyguardBottomAreaView: WeakReference<View>? = null

        // from com.sevtinge.cemiuiler.module.systemui.lockscreen.AddBlurEffectToLockScreen

        val keyguardBottomAreaViewClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView"
        ) ?: return

        XposedBridge.hookAllMethods(
            keyguardBottomAreaViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mLeftAffordanceView = WeakReference(
                        getValueByField(
                            param.thisObject,
                            "mLeftAffordanceView"
                        ) as ImageView
                    )
                    mRightAffordanceView = WeakReference(
                        getValueByField(
                            param.thisObject,
                            "mRightAffordanceView"
                        ) as ImageView
                    )
                    keyguardBottomAreaView = WeakReference(param.thisObject as View)
                }
            })

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val context = keyguardBottomAreaView?.get()?.context ?: return
                val keyguardManager =
                    context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (keyguardManager.isKeyguardLocked) {
                    val leftBlurDrawable = createBlurDrawable(
                        keyguardBottomAreaView!!.get()!!,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
                    val rightBlurDrawable = createBlurDrawable(
                        keyguardBottomAreaView!!.get()!!,
                        40,
                        100,
                        Color.argb(60, 255, 255, 255)
                    )
                    val rightLayerDrawable = LayerDrawable(arrayOf(rightBlurDrawable))
                    leftLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    rightLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                    mLeftAffordanceView?.get()?.background = leftLayerDrawable
                    mRightAffordanceView?.get()?.background = rightLayerDrawable
                } else {
                    mLeftAffordanceView?.get()?.background = null
                    mRightAffordanceView?.get()?.background = null
                }
            }
        }, 0, 100)
    }
}
