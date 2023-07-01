package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
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
        var handler = Handler(Looper.getMainLooper())
        var needUpdate = true

        // from com.sevtinge.cemiuiler.module.systemui.lockscreen.AddBlurEffectToLockScreen

        val keyguardBottomAreaViewClass =
            findClassIfExists("com.android.systemui.statusbar.phone.KeyguardBottomAreaView") ?: return

        XposedBridge.hookAllMethods(
            keyguardBottomAreaViewClass,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    mLeftAffordanceView = WeakReference(
                        getValueByField(param.thisObject, "mLeftAffordanceView") as ImageView
                    )
                    mRightAffordanceView = WeakReference(
                        getValueByField(param.thisObject, "mRightAffordanceView") as ImageView
                    )
                    keyguardBottomAreaView = WeakReference(param.thisObject as View)
                }
            })

        fun createBlurLayerDrawable(view: View, color: Int): Array<LayerDrawable> {
            val blurDrawable = createBlurDrawable(view, 40, 100, color)
            val leftLayerDrawable = LayerDrawable(arrayOf(blurDrawable)).apply {
                setLayerInset(0, 40, 40, 40, 40) }
            val rightLayerDrawable = LayerDrawable(arrayOf(blurDrawable)).apply {
                setLayerInset(0, 40, 40, 40, 40) }
            return arrayOf(leftLayerDrawable, rightLayerDrawable)
        }

        // 已摆烂，剩下数组异常问题（会多生成一个 0 的数组，导致抛出 ArrayIndexOutOfBoundsException 异常），感觉我没能力修好（
        // 性能优化得差不多了，但是在 1.2.120 正式版上还是会移除它，等找到有效解决方法再重新加回
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val context = keyguardBottomAreaView?.get()?.context ?: return
                val keyguardManager =
                    context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (keyguardManager.isKeyguardLocked) {
                    if (needUpdate) {
                        val message = Message.obtain()
                        message.obj = true
                        handler.sendMessage(message)
                        needUpdate = false
                    }
                } else {
                    if (needUpdate) {
                        val message = Message.obtain()
                        message.obj = false
                        handler.sendMessage(message)
                        needUpdate = false
                    }
                }
            }
        }, 0, 100)

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val updateFlag = msg.obj as? Boolean
                if (updateFlag != null) {
                    if (updateFlag) {
                        val blurColor = Color.argb(60, 255, 255, 255)
                        keyguardBottomAreaView?.get()?.let { view ->
                            val layerDrawables = createBlurLayerDrawable(view, blurColor)
                            mLeftAffordanceView?.get()?.background = layerDrawables[0]
                            mRightAffordanceView?.get()?.background = layerDrawables[1]
                        }
                    } else {
                        mLeftAffordanceView?.get()?.background = null
                        mRightAffordanceView?.get()?.background = null
                    }
                    needUpdate = true
                }
            }
        }
    }
}
