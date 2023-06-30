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

        fun createBlurLayerDrawable(view: View, color: Int): LayerDrawable {
            val blurDrawable = createBlurDrawable(view, 40, 100, color)
            return LayerDrawable(arrayOf(blurDrawable)).apply {
                setLayerInset(0, 40, 40, 40, 40)
            }
        }

        var handler = Handler(Looper.getMainLooper())
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val context = keyguardBottomAreaView?.get()?.context ?: return
                val keyguardManager =
                    context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (keyguardManager.isKeyguardLocked) {
                    val blurColor = Color.argb(60, 255, 255, 255)
                    // 调用函数来创建模糊图像
                    keyguardBottomAreaView?.get()?.let { view ->
                        val leftLayerDrawable = createBlurLayerDrawable(view, blurColor)
                        val rightLayerDrawable = createBlurLayerDrawable(view, blurColor)

                        // 通过Handler对象来发送一个消息
                        val message = Message.obtain()
                        message.obj = arrayOf(leftLayerDrawable, rightLayerDrawable)
                        handler.sendMessage(message)
                    }
                } else {
                    val message = Message.obtain()
                    message.obj = null
                    handler.sendMessage(message)
                }
            }
        }, 0, 100)

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.obj == null) {
                    mLeftAffordanceView?.get()?.background = null
                    mRightAffordanceView?.get()?.background = null
                } else {
                    val drawables = msg.obj as Array<LayerDrawable>
                    mLeftAffordanceView?.get()?.background = drawables[0]
                    mRightAffordanceView?.get()?.background = drawables[1]
                }
            }
        }
    }
}
