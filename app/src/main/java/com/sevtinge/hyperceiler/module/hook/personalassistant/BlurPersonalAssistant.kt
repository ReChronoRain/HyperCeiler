package com.sevtinge.hyperceiler.module.hook.personalassistant

import android.graphics.drawable.ColorDrawable
import android.view.Window
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.api.BlurDraw
import kotlin.math.abs

object BlurPersonalAssistant : BaseHook() {
    private val blurRadius by lazy {
        mPrefsMap.getInt("personal_assistant_blurradius", 80)
    }
    private val backgroundColor by lazy {
        mPrefsMap.getInt("personal_assistant_color", -1)
    }

    override fun init() {
        // val appVersionName = Helpers.getPackageVersionName(lpparam)
        var lastBlurRadius = -1
        /*val mScrollStateManager = mPersonalAssistantResultMethodsMap["ScrollStateManager"]!!

        for (descriptor in mScrollStateManager) {
            runCatching {
                val mScrollStateManagerMethod = descriptor.getMethodInstance(lpparam.classLoader)
                logI("mScrollStateManager method is $mScrollStateManagerMethod")
                XposedBridge.hookMethod(mScrollStateManagerMethod,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val scrollX = param.args[0] as Float
                            val window: Any?
                            window = if(appVersionName.contains("FOR-DESIGNER")) {
                                log("is designer ver.")
                                HookUtils.getValueByField(param.thisObject, "e") ?: return
                            } else {
                                HookUtils.getValueByField(param.thisObject, "b") ?: return
                            }
                            if (window.javaClass.name.contains("Window")) {
                                runCatching {
                                    window as Window
                                    val blurRadius = (scrollX * blurRadius).toInt()
                                    if (abs(blurRadius - lastBlurRadius) > 2) {
                                        window.setBackgroundBlurRadius(blurRadius)
                                        lastBlurRadius = blurRadius
                                    }
                                    val backgroundColorDrawable = ColorDrawable(backgroundColor)
                                    backgroundColorDrawable.alpha = (scrollX * 255).toInt()
                                    window.setBackgroundDrawable(backgroundColorDrawable)
                                }
                            }
                        }
                    })
            }
        }*/

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ScrollStateManager")
            }
        }.forEach { methodData ->
            methodData.getMethodInstance(lpparam.classLoader).createHook {
                after {
                val scrollX = it.args[0] as Float
                    val fieldNames = ('a'..'z').map { name -> name.toString() }
                    val window = BlurDraw.getValueByFields(it.thisObject, fieldNames) ?: return@after

                    if (window.javaClass.name.contains("Window")) {
                        runCatching {
                            window as Window
                            val blurRadius = (scrollX * blurRadius).toInt()
                            if (abs(blurRadius - lastBlurRadius) > 2) {
                                window.setBackgroundBlurRadius(blurRadius)
                                lastBlurRadius = blurRadius
                            }
                            val backgroundColorDrawable = ColorDrawable(backgroundColor)
                            backgroundColorDrawable.alpha = (scrollX * 255).toInt()
                            window.setBackgroundDrawable(backgroundColorDrawable)
                        }
                    }
                }
            }
        }

        /*
        val AssistantOverlayWindowClass = findClassIfExists(
            "com.miui.personalassistant.core.overlay.AssistantOverlayWindow"
        ) ?: return
        XposedHelpers.findAndHookMethod(
            AssistantOverlayWindowClass,
            "a",
            Float::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val scrollX = param.args[0] as Float
                    val window = HookUtils.getValueByField(param.thisObject, "b") ?: return
                    if (window.javaClass.name.contains("Window")) {
                        try {
                            window as Window
                            val blurRadius = (scrollX * blurRadius).toInt()
                            if (abs(blurRadius - lastBlurRadius) > 2) {
                                window.setBackgroundBlurRadius(blurRadius)
                                lastBlurRadius = blurRadius
                            }
                            val backgroundColorDrawable = ColorDrawable(backgroundColor)
                            backgroundColorDrawable.alpha = (scrollX * 255).toInt()
                            window.setBackgroundDrawable(backgroundColorDrawable)
                        } catch (e: Throwable) {
                            // 重复报错会污染日志
                            //  HookUtils.log(e.message)
                        }
                    }
                }
            })
         */
    }
}
