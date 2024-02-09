/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.blur.HookUtils.createBlurDrawable
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion

object BlurButton : BaseHook() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun init() {
        lateinit var mLeftAffordanceView: ImageView
        lateinit var mRightAffordanceView: ImageView
        lateinit var keyguardBottomAreaView: View

        if (isMoreHyperOSVersion(1f)) {
            loadClassOrNull(
                "com.android.keyguard.injector.KeyguardBottomAreaInjector"
            )!!.methodFinder()
                .filter {
                    name in setOf(
                        "updateLeftIcon",
                        "updateRightIcon"
                    )
                }.toList().createHooks {
                    after { param ->
                        mLeftAffordanceView =
                            ObjectUtils.getObjectOrNullAs<ImageView>(
                                param.thisObject,
                                "mLeftButton"
                            )!!

                        mRightAffordanceView =
                            ObjectUtils.getObjectOrNullAs<ImageView>(
                                param.thisObject,
                                "mRightButton"
                            )!!

                        // Your blur logic
                        val context = ObjectUtils.getObjectOrNull(param.thisObject, "mContext") as Context
                        val keyguardManager =
                            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                        if (keyguardManager.isKeyguardLocked) {
                            val leftBlurDrawable = createBlurDrawable(
                                mLeftAffordanceView, 40, 100, Color.argb(60, 255, 255, 255)
                            )
                            val rightBlurDrawable = createBlurDrawable(
                                mRightAffordanceView, 40, 100, Color.argb(60, 255, 255, 255)
                            )
                            val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
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
        } else {
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
                            ObjectUtils.getObjectOrNullAs<ImageView>(
                                param.thisObject,
                                "mLeftAffordanceView"
                            )!!

                        mRightAffordanceView =
                            ObjectUtils.getObjectOrNullAs<ImageView>(
                                param.thisObject,
                                "mRightAffordanceView"
                            )!!

                        keyguardBottomAreaView = param.thisObject as View

                        // Your blur logic
                        val context = keyguardBottomAreaView.context ?: return@after
                        val keyguardManager =
                            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                        if (keyguardManager.isKeyguardLocked) {
                            val leftBlurDrawable = createBlurDrawable(
                                keyguardBottomAreaView, 40, 100, Color.argb(60, 255, 255, 255)
                            )
                            val rightBlurDrawable = createBlurDrawable(
                                keyguardBottomAreaView, 40, 100, Color.argb(60, 255, 255, 255)
                            )
                            val leftLayerDrawable = LayerDrawable(arrayOf(leftBlurDrawable))
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
        }

        /*var mLeftAffordanceView: WeakReference<ImageView>? = null
        var mRightAffordanceView: WeakReference<ImageView>? = null
        var keyguardBottomAreaView: WeakReference<View>? = null
        var handler = Handler(Looper.getMainLooper())
        var needUpdate = true

        // from com.sevtinge.hyperceiler.module.systemui.lockscreen.AddBlurEffectToLockScreen

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
