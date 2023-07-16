package com.sevtinge.cemiuiler.module.systemui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils.createBlurDrawable

object BlurButton : BaseHook() {
    override fun init() {
        lateinit var mLeftAffordanceView: ImageView
        lateinit var mRightAffordanceView: ImageView
        lateinit var keyguardBottomAreaView: View

        loadClassOrNull(
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView"
        )!!.methodFinder()
            .filter {
                name in setOf(
                    "onAttachedToWindow",
                    "onDetachedFromWindow",
                    "updateRightAffordanceIcon",
                    "updateLeftAffordanceIcon"
                )
            }.toList().createHooks {
                after { param ->
                    mLeftAffordanceView =
                        ObjectUtils.getObjectOrNullAs<ImageView>(param.thisObject, "mLeftAffordanceView")!!

                    mRightAffordanceView =
                        ObjectUtils.getObjectOrNullAs<ImageView>(param.thisObject, "mRightAffordanceView")!!

                    keyguardBottomAreaView = param.thisObject as View

                    // Your blur logic
                    val context = keyguardBottomAreaView.context ?: return@after
                    val keyguardManager =
                        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                    if (keyguardManager.isKeyguardLocked) {
                        val leftBlurDrawable = createBlurDrawable(
                            keyguardBottomAreaView, 40, 100, Color.argb(60, 255, 255, 255)
                        )
                        val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
                        val rightBlurDrawable = createBlurDrawable(
                            keyguardBottomAreaView, 40, 100, Color.argb(60, 255, 255, 255)
                        )
                        val rightLayerDrawable = LayerDrawable(arrayOf(rightBlurDrawable))
                        leftLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                        rightLayerDrawable.setLayerInset(0, 40, 40, 40, 40)
                        mLeftAffordanceView.background = leftLayerDrawable
                        mRightAffordanceView.background = rightLayerDrawable
                    } else {
                        mLeftAffordanceView.background = null
                        mRightAffordanceView.background = null
                    }
                }
            }

        /*var mLeftAffordanceView: WeakReference<ImageView>? = null
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
        // 性能优化得差不多了
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
        }*/
    }
}
