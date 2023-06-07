package com.sevtinge.cemiuiler.module.personalassistant

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlin.math.abs

object BlurPersonalAssistant : BaseHook() {
    val blurRadius = mPrefsMap.getInt("personal_assistant_blurradius", 80)
    val backgroundColor = mPrefsMap.getInt("personal_assistant_color", -1)

    override fun init() {
        val AssistantOverlayWindowClass = findClassIfExists(
            "com.miui.personalassistant.core.overlay.AssistantOverlayWindow"
        ) ?: return

        var lastBlurRadius = -1

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
    }
}
