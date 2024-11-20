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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.os2

import android.animation.ValueAnimator.*
import android.content.*
import android.database.*
import android.net.*
import android.os.*
import android.provider.Settings.*
import android.util.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.*
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FlashLight.FlashBrightness.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.log.*
import com.sevtinge.hyperceiler.utils.shell.*
import de.robv.android.xposed.*
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.io.*


object NewFlashLight : TileUtils() {
    private const val mtk: String = "/sys/class/flashlight_core/flashlight/torchbrightness"
    private const val torch: String = "/sys/class/leds/led:torch_0/brightness"
    private const val other: String = "/sys/class/leds/flashlight/brightness"
    private const val flashSwitch: String = "/sys/class/leds/led:switch_0/brightness"
    private const val maxBrightness: String = "/sys/class/leds/led:torch_0/max_brightness"
    private var mode: Int = 0
    private var lastFlash: Int = -1
    private var isListening: Boolean = false
    private var isHook: Boolean = false

    private val BrightnessUtils by lazy {
        loadClassOrNull("com.android.systemui.controlcenter.policy.BrightnessUtils", lpparam.classLoader)!!
    }

    override fun init() {
        super.init()
        mode = mPrefsMap.getStringAsInt("security_flash_light_switch", 0)
        setPermission(mtk)
        setPermission(torch)
        setPermission(other)
        initListen()
        hookBrightness()
    }

    override fun customClass(): Class<*> {
        return loadClassOrNull("com.android.systemui.qs.tiles.MiuiFlashlightTile")!!
    }

    override fun tileUpdateState(
        param: MethodHookParam,
        mResourceIcon: Class<*>?,
        tileName: String?
    ): ArrayMap<String, Int>? {
        val mContext = param.thisObject.getObjectField("mContext") as Context
        if (param.args[1] != null) {
            val enabled = param.args[1] as Boolean
            val flash = param.thisObject.getObjectField("flashlightController")
            val isEnabled = flash!!.callMethod("isEnabled") as Boolean
            if (enabled || isEnabled) {
                setFlashLightEnabled(mContext, 1) // 我不李姐
            } else {
                setFlashLightEnabled(mContext, 0)
            }
        }
        return null
    }

    private fun initListen() {
        hookAllConstructors(
            "com.android.systemui.controlcenter.policy.MiuiBrightnessController",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val mContext =
                        param.thisObject.getObjectFieldOrNullAs<Context>("mContext")
                    if (mContext != null) {
                        listening(mContext, param)
                    }
                }
            }
        )
    }

    fun listening(mContext: Context, param: MethodHookParam) {
        if (!isListening) {
            val contentObserver: ContentObserver =
                object : ContentObserver(Handler(mContext.mainLooper)) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        super.onChange(selfChange, uri)
                        if (lastFlash != -1) lastFlash = -1
                        isHook = isFlashLightEnabled(mContext)
                        if (isHook) {
                            if (ShellUtils.safeExecCommandWithRoot("settings get system flash_light_brightness") == "null") ShellUtils.safeExecCommandWithRoot("settings put system flash_light_brightness 0")
                            val b = getFlashBrightness(mContext)
                            if (b != null) {
                                val mObject = restore(b)
                                val flash = getBrightness(mObject)
                                val slider = getSlider(mObject)
                                sliderAnimator(slider, param)
                                writeFile(flash)
                            }
                        }
                    }
                }
            mContext.contentResolver.registerContentObserver(
                System.getUriFor("flash_light_enabled"),
                false, contentObserver
            )
            this.isListening = true
        }
    }

    private fun hookBrightness() {
        loadClassOrNull("com.android.systemui.controlcenter.policy.MiuiBrightnessController\$\$ExternalSyntheticLambda0")!!
            .methodFinder().filterByName("run")
            .first().hookBeforeMethod {
                if (isHook) it.result = null
            }

        loadClassOrNull("com.android.systemui.controlcenter.policy.MiuiBrightnessController\$2")!!
            .methodFinder().filterByName("run")
            .first().hookBeforeMethod {
                if (isHook) it.result = null
            }

        hookStop()
        hookBrightnessUtils()
    }

    private fun hookStop() {
        loadClassOrNull("com.android.systemui.controlcenter.policy.MiuiBrightnessController")!!
            .methodFinder().filterByName("onStop")
            .filterByParamTypes(Int::class.javaPrimitiveType)
            .first().hookBeforeMethod {
                if (isHook) {
                    val mContext =
                        it.thisObject.getObjectField("mContext") as Context
                    if (lastFlash != -1) {
                        val mObject =
                            FlashLight.FlashBrightness(it.args[0] as Int, lastFlash).toJSON()
                        setFlashBrightness(mContext, mObject.toString())
                    }
                }
            }

    }

    private fun sliderAnimator(i: Int, param: MethodHookParam) {
        val isUserSliding =
            param.thisObject.getObjectField("isUserSliding") as Boolean
        val toggleSliderBaseControllerImpl =
            param.thisObject.getObjectField("mToggleSlidersController")
        val toggleSliderBase =
            toggleSliderBaseControllerImpl!!.getObjectField("toggleSliders")
        if (!isUserSliding && !(toggleSliderBase!!.callMethod("isEmpty") as Boolean)) {
            val mControlValueInitialized =
                param.thisObject.getObjectField("mControlValueInitialized") as Boolean
            if (!mControlValueInitialized) {
                toggleSliderBaseControllerImpl.callMethod("setValue", i)
                param.thisObject.setObjectField("mControlValueInitialized", true)
            }
            val ofInt = ofInt(
                toggleSliderBaseControllerImpl.getObjectField("sliderValue") as Int, i
            )
            param.thisObject.setObjectField("mSliderAnimator", ofInt)
            ofInt.addUpdateListener { animation ->
                param.thisObject.setObjectField("mExternalChange", true)
                toggleSliderBaseControllerImpl.callMethod("setValue", animation.animatedValue)
                param.thisObject.setObjectField("mExternalChange", false)
            }
            ofInt.setDuration(3000)
            ofInt.start()
        }
    }

    private fun hookBrightnessUtils() {
        try {
            BrightnessUtils.getDeclaredMethod(
                "convertGammaToLinearFloat",
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            convertGammaToLinearFloat(BrightnessUtils, true)
        } catch (e: NoSuchMethodException) {
            try {
                BrightnessUtils.getDeclaredMethod(
                    "convertGammaToLinearFloat",
                    Float::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                convertGammaToLinearFloat(BrightnessUtils, false)
            } catch (ex: NoSuchMethodException) {
                logE(TAG, lpparam.packageName, "Find Method convertGammaToLinearFloat is null!!")
            }
        }
    }

    private fun convertGammaToLinearFloat(clz: Class<*>, b: Boolean) {
        val maxBrightness = maxBrightness()
        clz.methodFinder().filterByName("convertGammaToLinearFloat")
            .filterByParamTypes(
                if (b) Int::class.javaPrimitiveType else Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                if (b) Float::class.javaPrimitiveType else Int::class.javaPrimitiveType
            ).first().hookBeforeMethod {
                if (isHook) {
                    // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat int 1: " + param.args[0]);
                    // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat float 2: " + param.args[1]);
                    // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat float 3: " + param.args[2]);
                    var min = it.args[if (b) 1 else 0] as Float
                    var max = it.args[if (b) 2 else 1] as Float
                    if (min < 0.001f) {
                        min = 0.00114514f
                    }
                    min = Math.round(min * 500).toFloat()
                    max = Math.round(max * 500).toFloat()
                    val exp: Float
                    val GAMMA_SPACE_MAX =
                        XposedHelpers.getStaticIntField(BrightnessUtils, "GAMMA_SPACE_MAX")
                    val R = XposedHelpers.getStaticFloatField(BrightnessUtils, "R")
                    val A = XposedHelpers.getStaticFloatField(BrightnessUtils, "A")
                    val B = XposedHelpers.getStaticFloatField(BrightnessUtils, "B")
                    val C = XposedHelpers.getStaticFloatField(BrightnessUtils, "C")
                    val norm =
                        MathUtils.norm(0.0f, GAMMA_SPACE_MAX.toFloat(), (it.args[if (b) 0 else 2] as Int).toFloat())
                    exp = if (norm <= R)
                        MathUtils.sq(norm / R)
                    else
                        MathUtils.exp((norm - C) / A) + B
                    if (min < 10)  min = 12f
                    // logE("FlashLight", "convertGammaToLinearFloat R: " + R + " A: " + A + " B: " + B + " C: " + C);
                    // logE("FlashLight", "convertGammaToLinearFloat exp: " + exp);
                    val end =
                        MathUtils.lerpNew(min, max, (MathUtils.constrain(exp, 0.0f, 12.0f) / 12.0f))
                    // logE("FlashLight", "convertGammaToLinearFloat min: " + min);
                    // logE("FlashLight", "convertGammaToLinearFloat max: " + max);
                    // logE("FlashLight", "convertGammaToLinearFloat end: " + end);
                    var i = Math.round(end)
                    if (i != 0) {
                        if (maxBrightness != -1 && i > maxBrightness) {
                            i = maxBrightness
                        }
                        // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat i: " + i);
                        lastFlash = i
                        writeFile(i)
                    }
                    it.result = end
                }
            }
    }

    private fun maxBrightness(): Int {
        var line: String?
        var reader: BufferedReader? = null
        var builder: StringBuilder? = null
        val file = File(maxBrightness)
        if (file.exists()) {
            try {
                reader = BufferedReader(FileReader(maxBrightness))
                builder = StringBuilder()
                while ((reader.readLine().also { line = it }) != null) {
                    builder.append(line)
                }
            } catch (e: IOException) {
                logE(TAG, lpparam.packageName, "Error to read: $maxBrightness", e)
            } finally {
                try {
                    reader?.close()
                } catch (e: IOException) {
                    logE(TAG, lpparam.packageName, "Close reader error: ", e)
                }
            }
        } else {
            logE(TAG, lpparam.packageName, "Not Found FlashLight File: $maxBrightness")
        }

        if (builder != null) {
            try {
                return builder.toString().toInt()
            } catch (e: NumberFormatException) {
                logE(TAG, lpparam.packageName, "To int E: $e")
            }
        }
        return -1
    }

    private fun writeFile(flash: Int) {
        val bmtk = exists(mtk)
        val btorch = exists(torch)
        val bother = exists(other)
        when (mode) {
            0, 1 -> {
                if (bmtk) write(mtk, flash)
                if (btorch) write(torch, flash)
                if (bother) write(other, flash)
            }

            2 -> {
                if (bmtk) zero(mtk, flash)
                if (bother) {
                    zero(other, flash)
                }
                if (btorch) zero(torch, flash)
            }

            3 -> {
                if (bmtk) flashSwitch(mtk, flash)

                if (bother) {
                    flashSwitch(other, flash)
                }
                if (btorch) flashSwitch(torch, flash)
            }
        }
    }

    private fun zero(path: String, flash: Int) {
        write(path, 0)
        write(path, flash)
    }

    private fun flashSwitch(path: String, flash: Int) {
        write(path, flash)
        write(flashSwitch, 1)
        write(flashSwitch, 0)
    }

    private fun exists(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    private fun write(path: String, flash: Int) {
        try {
            FileWriter(path, false).use { writer ->
                writer.write(flash.toString())
                writer.flush()
            }
        } catch (e: IOException) {
            logE(TAG, lpparam.packageName, "write $path E: $e")
        }
    }

    private fun setPermission(paths: String) {
        try {
            val checkCommand = "test -e $paths && echo exists || echo not_found"
            val result = ShellUtils.safeExecCommandWithRoot(checkCommand)
            if (result.contains("not_found")) {
                logE(TAG, lpparam.packageName, "SetPermission: Path $paths does not exist")
                return
            }

            val chmodCommand = "chmod 777 $paths"
            val chmodResult = ShellUtils.safeExecCommandWithRoot(chmodCommand)

            if (chmodResult.isEmpty()) {
                logI(TAG, lpparam.packageName, "SetPermission: Successfully set permissions for $paths")
            } else {
                logE(TAG, lpparam.packageName, "SetPermission: Failed to set permissions for $paths. Error: $chmodResult")
            }
        } catch (e: Exception) {
            logE(TAG, lpparam.packageName, "SetPermission: Exception while processing $paths. Error: $e")
        }
    }

    private fun isFlashLightEnabled(context: Context): Boolean {
        try {
            return System.getInt(context.contentResolver, "flash_light_enabled") == 1
        } catch (e: SettingNotFoundException) {
            logE(TAG, lpparam.packageName, "not found flash_light_enabled: $e")
            setFlashLightEnabled(context, 0)
            return false
        }
    }

    private fun setFlashLightEnabled(context: Context, set: Int) {
        System.putInt(context.contentResolver, "flash_light_enabled", set)
    }

    private fun getFlashBrightness(context: Context): String {
        return System.getString(context.contentResolver, "flash_light_brightness")
    }

    private fun setFlashBrightness(context: Context, set: String) {
        System.putString(context.contentResolver, "flash_light_brightness", set)
    }
}

