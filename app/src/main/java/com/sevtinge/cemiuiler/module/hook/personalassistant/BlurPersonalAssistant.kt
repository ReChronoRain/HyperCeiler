package com.sevtinge.cemiuiler.module.hook.personalassistant

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.personalassistant.PersonalAssistantDexKit.mPersonalAssistantResultMethodsMap
import com.sevtinge.cemiuiler.utils.api.BlurDraw.getValueByFields
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import kotlin.math.abs

object BlurPersonalAssistant : BaseHook() {
    val blurRadius = mPrefsMap.getInt("personal_assistant_blurradius", 80)
    val backgroundColor = mPrefsMap.getInt("personal_assistant_color", -1)

    override fun init() {
        // val appVersionName = Helpers.getPackageVersionName(lpparam)
        var lastBlurRadius = -1
        val mScrollStateManager = mPersonalAssistantResultMethodsMap["ScrollStateManager"]!!

        for (descriptor in mScrollStateManager) {
            runCatching {
                val mScrollStateManagerMethod = descriptor.getMethodInstance(lpparam.classLoader)
                logI("mScrollStateManager method is $mScrollStateManagerMethod")
                XposedBridge.hookMethod(mScrollStateManagerMethod,
                    object : XC_MethodHook() {
                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val scrollX = param.args[0] as Float
                            /*val window: Any?
                            window = if(appVersionName.contains("FOR-DESIGNER")) {
                                log("is designer ver.")
                                HookUtils.getValueByField(param.thisObject, "e") ?: return
                            } else {
                                HookUtils.getValueByField(param.thisObject, "b") ?: return
                            }*/
                            val fieldNames = ('a'..'z').map { it.toString() }
                            val window = getValueByFields(param.thisObject, fieldNames) ?: return
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
                @RequiresApi(Build.VERSION_CODES.S)
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
