package com.sevtinge.hyperceiler.module.hook.personalassistant

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun init() {
        // val appVersionName = Helpers.getPackageVersionName(lpparam)
        var lastBlurRadius = -1

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
    }
}
