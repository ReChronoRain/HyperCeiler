/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils
import com.sevtinge.hyperceiler.libhook.utils.api.MathUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getStaticFloatField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getStaticIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/**
 * 手电筒亮度调节磁贴
 *
 * 功能：通过系统亮度滑块调节手电筒亮度
 */
object NewFlashLight : TileUtils() {

    // 手电筒亮度控制文件路径
    private const val MTK = "/sys/class/flashlight_core/flashlight/torchbrightness"
    private const val TORCH = "/sys/class/leds/led:torch_0/brightness"
    private const val OTHER = "/sys/class/leds/flashlight/brightness"
    private const val FLASH_SWITCH = "/sys/class/leds/led:switch_0/brightness"
    private const val MAX_BRIGHTNESS = "/sys/class/leds/led:torch_0/max_brightness"

    // Settings keys
    private const val SETTING_FLASH_ENABLED = "flash_light_enabled"
    private const val SETTING_FLASH_BRIGHTNESS = "flash_light_brightness"

    private var mode: Int = 0
    private var lastFlash: Int = -1
    private var isListening: Boolean = false
    private var isHook: Boolean = false
    private var brightnessObserver: ContentObserver? = null

    override fun onCreateTileConfig(): TileConfig {
        return TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.MiuiFlashlightTile"))
            .build()
    }

    override fun init() {
        super.init()
        // 读取配置
        mode = mPrefsMap.getStringAsInt("security_flash_light_switch", 0)

        // 设置文件权限
        setPermission(MTK)
        setPermission(TORCH)
        setPermission(OTHER)

        // Hook 相关方法
        initBrightnessControllerHook()
        hookBrightnessControl()
        hookBrightnessUtils()
    }

    override fun onUpdateState(ctx: TileContext): TileState? {
        val context = ctx.context
        val flashController = ctx.getField<Any>("flashlightController")
        val isEnabled = flashController?.callMethod("isEnabled") as? Boolean ?: false

        // 同步手电筒状态到 Settings
        if (isEnabled) {
            setFlashLightEnabled(context, 1)
        } else {
            setFlashLightEnabled(context, 0)
        }

        // 返回 null 使用原有状态逻辑
        return null
    }

    /**
     * Hook 亮度控制器构造函数，添加监听
     */
    private fun initBrightnessControllerHook() {
        findClass("com.android.systemui.controlcenter.policy.MiuiBrightnessController")
            .hookAllConstructors {
                after { param ->
                    val context = getObjectField(param.thisObject, "mContext") as? Context
                    if (context != null) {
                        setupBrightnessListener(context, param.thisObject)
                    }
                }
            }
    }

    /**
     * 设置亮度监听器
     */
    private fun setupBrightnessListener(context: Context, controller: Any) {
        if (isListening) {
            XposedLog.d(TAG, "Already listening")
            return
        }

        brightnessObserver = object : ContentObserver(Handler(context.mainLooper)) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                lastFlash = -1
                isHook = isFlashLightEnabled(context)

                if (isHook) {
                    val brightness = getFlashBrightness(context)

                    if (brightness != null && brightness != "0") {
                        try {
                            val jsonObject = JSONObject(brightness)
                            val flash = jsonObject.getInt("brightness")
                            val slider = jsonObject.getInt("slider")

                            setSliderValue(controller, slider)
                            // 写入亮度值
                            writeFile(flash)
                        } catch (e: JSONException) {
                            XposedLog.e(TAG, "Failed to parse brightness JSON", e)
                        }
                    }
                }
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_FLASH_ENABLED),
            false,
            brightnessObserver!!
        )
        isListening = true
        XposedLog.i(TAG, "Brightness listener set up successfully")
    }

    /**
     * Hook 亮度控制相关方法
     */
    private fun hookBrightnessControl() {
        val lambdaClass = findClassIfExists(
            $$$"com.android.systemui.controlcenter.policy.MiuiBrightnessController$$ExternalSyntheticLambda0"
        )
        lambdaClass?.let {
            it.beforeHookMethod("run") { param ->
                if (isHook) {
                    param.result = null
                }
            }
        }

        val innerClass = findClassIfExists(
            $$"com.android.systemui.controlcenter.policy.MiuiBrightnessController$2"
        )
        innerClass?.let {
            it.beforeHookMethod("run") { param ->
                if (isHook) {
                    param.result = null
                }
            }
        }

        findClass("com.android.systemui.controlcenter.policy.MiuiBrightnessController")
            .beforeHookMethod("onStop", Int::class.java) { param ->
                if (isHook && lastFlash != -1) {
                    val context = getObjectField(param.thisObject, "mContext") as Context
                    val slider = param.args[0] as Int

                    val jsonObject = JSONObject().apply {
                        put("slider", slider)
                        put("brightness", lastFlash)
                    }
                    setFlashBrightness(context, jsonObject.toString())
                }
            }
    }

    /**
     * Hook BrightnessUtils 亮度转换方法
     */
    private fun hookBrightnessUtils() {
        val brightnessUtils = findClassIfExists(
            "com.android.systemui.controlcenter.policy.BrightnessUtils"
        ) ?: return

        val hookGammaConversion = { paramOrder: Boolean ->
            brightnessUtils.beforeHookMethod(
                "convertGammaToLinearFloat",
                if (paramOrder) Int::class.java else Float::class.java,
                Float::class.java,
                if (paramOrder) Float::class.java else Int::class.java,
            ) { param ->
                if (!isHook) return@beforeHookMethod

                var min = param.args[if (paramOrder) 1 else 0] as Float
                var max = param.args[if (paramOrder) 2 else 1] as Float
                val value = param.args[if (paramOrder) 0 else 2] as Int

                // 调整最小值
                if (min < 0.001f) {
                    min = 0.00114514f
                }
                min = (min * 500).roundToInt().toFloat()
                max = (max * 500).roundToInt().toFloat()

                // 计算亮度值
                val brightness = calculateBrightness(brightnessUtils, value, min, max)

                if (brightness > 0) {
                    lastFlash = brightness
                    writeFile(brightness)
                }

                param.result = brightness.toFloat()
            }
        }

        // 尝试两种参数顺序
        runCatching {
            brightnessUtils.getDeclaredMethod(
                "convertGammaToLinearFloat",
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            hookGammaConversion(true)
        }.recoverCatching {
            brightnessUtils.getDeclaredMethod(
                "convertGammaToLinearFloat",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            hookGammaConversion(false)
        }.onFailure {
            XposedLog.e(TAG, "convertGammaToLinearFloat method not found")
        }
    }

    /**
     * 计算亮度值
     */
    private fun calculateBrightness(
        brightnessUtils: Class<*>,
        value: Int,
        min: Float,
        max: Float
    ): Int {
        val gammaSpaceMax = getStaticIntField(brightnessUtils, "GAMMA_SPACE_MAX")
        val r = getStaticFloatField(brightnessUtils, "R")
        val a = getStaticFloatField(brightnessUtils, "A")
        val b = getStaticFloatField(brightnessUtils, "B")
        val c = getStaticFloatField(brightnessUtils, "C")

        val norm = MathUtils.norm(0.0f, gammaSpaceMax.toFloat(), value.toFloat())
        val exp = if (norm <= r) {
            MathUtils.sq(norm / r)
        } else {
            MathUtils.exp((norm - c) / a) + b
        }

        val finalMin = if (min < 10) 12f else min
        val end = MathUtils.lerpNew(finalMin, max, MathUtils.constrain(exp, 0.0f, 12.0f) / 12.0f)

        var brightness = end.roundToInt()
        val maxBrightness = getMaxBrightness()
        if (maxBrightness != -1 && brightness > maxBrightness) {
            brightness = maxBrightness
        }

        return brightness
    }

    /**
     * 动画设置滑块位置
     */
    private fun setSliderValue(controller: Any, targetValue: Int) {
        runCatching {
            val isUserSliding = getObjectField(controller, "isUserSliding") as? Boolean ?: false
            val sliderController = getObjectField(controller, "mToggleSlidersController") ?: return

            if (!isUserSliding) {
                val initialized = getObjectField(controller, "mControlValueInitialized") as? Boolean ?: false

                if (!initialized) {
                    // 尝试设置值
                    runCatching {
                        callMethod(sliderController, "setValue", targetValue, false)
                    }.recoverCatching {
                        callMethod(sliderController, "setValue", targetValue)
                    }.recoverCatching {
                        setObjectField(sliderController, "sliderValue", targetValue)
                        refreshSliders(sliderController, targetValue)
                    }.onSuccess {
                        XposedLog.d(TAG, "Set slider value successfully")
                    }.onFailure {
                        XposedLog.e(TAG, "Failed to set slider value", it)
                    }

                    setObjectField(controller, "mControlValueInitialized", true)
                }
            }
        }.onFailure {
            XposedLog.e(TAG, "Error in setSliderValue", it)
        }
    }

    private fun refreshSliders(sliderController: Any, value: Int) {
        runCatching {
            val toggleSliders = getObjectField(sliderController, "toggleSliders") ?: return
            val iterator = callMethod(toggleSliders, "iterator") as? Iterator<*> ?: return
            iterator.forEach { slider ->
                runCatching {
                    slider?.let {
                        callMethod(it, "setValue", value)
                    }
                }
            }
        }
    }

    /**
     * 读取最大亮度值
     */
    private fun getMaxBrightness(): Int {
        val file = File(MAX_BRIGHTNESS)
        if (!file.exists()) {
            XposedLog.e(TAG, "Max brightness file not found: $MAX_BRIGHTNESS")
            return -1
        }

        return try {
            file.readText().trim().toInt()
        } catch (e: Exception) {
            XposedLog.e(TAG, "Failed to read max brightness", e)
            -1
        }
    }

    /**
     * 写入亮度值到文件
     */
    private fun writeFile(flash: Int) {
        val brightMTK = File(MTK).exists()
        val brightTorch = File(TORCH).exists()
        val brightOther = File(OTHER).exists()

        when (mode) {
            0, 1 -> {
                if (brightMTK) write(MTK, flash)
                if (brightTorch) write(TORCH, flash)
                if (brightOther) write(OTHER, flash)
            }
            2 -> {
                if (brightMTK) zero(MTK, flash)
                if (brightOther) {
                    zero(OTHER, flash)
                } else if (brightTorch) {
                    zero(TORCH, flash)
                }
            }
            3 -> {
                if (brightMTK) flashSwitch(MTK, flash)
                if (brightOther) {
                    flashSwitch(OTHER, flash)
                } else if (brightTorch) {
                    flashSwitch(TORCH, flash)
                }
            }
        }
    }

    private fun zero(path: String, flash: Int) {
        write(path, 0)
        write(path, flash)
    }

    private fun flashSwitch(path: String, flash: Int) {
        write(path, flash)
        write(FLASH_SWITCH, 1)
        write(FLASH_SWITCH, 0)
    }

    private fun write(path: String, value: Int) {
        try {
            File(path).writeText(value.toString())
        } catch (e: IOException) {
            XposedLog.e(TAG, "Failed to write $path", e)
        }
    }

    /**
     * 设置文件权限
     */
    private fun setPermission(path: String) {
        try {
            val checkCommand = "test -e $path && echo exists || echo not_found"
            val result = ShellUtils.execCommand(checkCommand, false).toString()

            if (result.contains("not_found")) {
                XposedLog.e(TAG, "Path does not exist: $path")
                return
            }

            val chmodCommand = "chmod 777 $path"
            val chmodResult = ShellUtils.execCommand(chmodCommand, false).toString()

            if (chmodResult.isEmpty()) {
                XposedLog.d(TAG, "Successfully set permissions for $path")
            } else {
                XposedLog.e(TAG, "Failed to set permissions for $path: $chmodResult")
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, "Exception setting permissions for $path", e)
        }
    }

    /**
     * Settings 辅助方法
     */
    private fun isFlashLightEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, SETTING_FLASH_ENABLED) == 1
        } catch (e: Settings.SettingNotFoundException) {
            setFlashLightEnabled(context, 0)
            false
        }
    }

    private fun setFlashLightEnabled(context: Context, value: Int) {
        Settings.System.putInt(context.contentResolver, SETTING_FLASH_ENABLED, value)
    }

    private fun getFlashBrightness(context: Context): String? {
        return try {
            Settings.System.getString(context.contentResolver, SETTING_FLASH_BRIGHTNESS)
        } catch (_: Throwable) {
            null
        }
    }

    private fun setFlashBrightness(context: Context, value: String) {
        Settings.System.putString(context.contentResolver, SETTING_FLASH_BRIGHTNESS, value)
    }
}
