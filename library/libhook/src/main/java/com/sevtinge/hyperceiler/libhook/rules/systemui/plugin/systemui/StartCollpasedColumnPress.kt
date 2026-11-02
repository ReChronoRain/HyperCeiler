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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getBooleanField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getFloatField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setLongField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object StartCollpasedColumnPress {
    private const val TAG = "StartCollpasedColumnPress"

    /**
     * 展开/收起触摸那个合成 lambda 的方法名。编译器给它的编号会随版本漂移：
     * 音量插件在 OS3 上叫 lambda$processExpandTouch$1，在 OS4（4.0.0.44 / 4.0.5.0）
     * 上叫 lambda$processExpandTouch$2。写死编号会让这条 hook 在其中一边找不到目标，
     * 而 findMethod 找不到时抛异常，会被 loadClassLoaders 的 runCatching 整个吞掉，
     * 连带本 loader 里后面的 hook 一起不生效。所以这里只按结构特征找：
     * 名字前缀 lambda$processExpandTouch$ + 无参 + void 返回（三套固件实测均为唯一命中）。
     */
    private const val EXPAND_TOUCH_LAMBDA_PREFIX = "lambda\$processExpandTouch\$"

    fun initLoaderHook(classLoader: ClassLoader) {
        val miuiVolumeDialogView by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogView", classLoader)
        }
        val miuiVolumeDialogMotion by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeDialogMotion", classLoader)
        }
        val miuiVolumeSeekBar by lazy {
            loadClass("com.android.systemui.miui.volume.MiuiVolumeSeekBar", classLoader)
        }

        var longClick = false
        var longPressJob: Job? = null
        BaseHook.registerHotReloadCleanup {
            longPressJob?.cancel()
            longPressJob = null
        }

        fun View.startScaleAnimation() {
            longClick = true
            animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(300)
                .start()
        }

        fun View.stopScaleAnimation() {
            longClick = false
            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .start()
        }

        miuiVolumeDialogView.findMethod { name("onFinishInflate") }.createAfterHook {
                    it.thisObject.getObjectFieldAs<View>("mExpandButton").apply {
                        alpha = 0f
                        isClickable = false
                        visibility = View.GONE
                        setOnClickListener(null)
                    }

                }
        miuiVolumeDialogView.findMethod { name("notifyAccessibilityChanged"); parameterTypes(Boolean::class.javaPrimitiveType!!) }.createAfterHook {
                    it.thisObject.getObjectFieldAs<View>("mExpandButton").apply {
                        isClickable = false
                        visibility = View.GONE
                        setOnClickListener(null)
                    }
                }

        hookProcessExpandTouchLambda(miuiVolumeDialogMotion)
        miuiVolumeSeekBar.findMethod { name("onTouchEvent"); parameterTypes(MotionEvent::class.java) }.createAfterHook {
                val mSeekBarOnclickListener = it.thisObject.getObjectField("mSeekBarOnclickListener")
                val mSeekBarAnimListener = it.thisObject.getObjectField("mSeekBarAnimListener")!!
                val volumePanelViewController = mSeekBarAnimListener.getObjectField($$"this$0")!!
                val mVolumeView = volumePanelViewController.getObjectFieldAs<View>("mVolumeView")

                it.thisObject.setLongField("mCurrentMS",0L)
                if (mSeekBarOnclickListener != null) {
                    val motionEvent = it.args[0] as MotionEvent
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if ( !volumePanelViewController.getBooleanField("mExpanded") ){
                                longPressJob = CoroutineScope(Dispatchers.Main).launch {
                                    mVolumeView.startScaleAnimation()
                                    delay(300)
                                    val mMoveY = it.thisObject.getFloatField("mMoveY")
                                    if (longClick && mMoveY < 10f){
                                        mVolumeView.apply {
                                            scaleY = 1f
                                            scaleX = 1f
                                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                        mSeekBarOnclickListener.callMethod( "onClick")

                                    }

                                }
                            }
                        }
                        MotionEvent.ACTION_UP->{
                            mVolumeView.stopScaleAnimation()
                            longPressJob?.cancel()
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            mVolumeView.stopScaleAnimation()
                            longPressJob?.cancel()
                        }
                    }

                }
            }

    }

    /**
     * 按结构特征定位“展开/收起触摸”那个合成 lambda 并挂前置 hook。
     * 编号在不同固件上会漂：OS3 = lambda$processExpandTouch$1，
     * OS4（4.0.0.44 / 4.0.5.0）= lambda$processExpandTouch$2，
     * 三套固件里“前缀 + 无参 + void”均为唯一命中。
     */
    private fun hookProcessExpandTouchLambda(hostClass: Class<*>?) {
        if (hostClass == null) {
            return
        }
        var hooked = 0
        for (method in hostClass.declaredMethods) {
            if (method.name.startsWith(EXPAND_TOUCH_LAMBDA_PREFIX)
                && method.parameterTypes.isEmpty()
                && method.returnType == Void.TYPE
            ) {
                BaseHook.hookMethod(method, object : IMethodHook {
                    override fun before(param: HookParam) {
                        val owner = param.thisObject ?: return
                        BaseHook.setBooleanField(owner, "mIsExpandButton", true)
                    }
                })
                hooked++
            }
        }
        if (hooked == 0) {
            XposedLog.d(TAG, "can't find processExpandTouch lambda in " + hostClass.name)
        }
    }
}
