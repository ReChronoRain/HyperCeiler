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
package com.sevtinge.hyperceiler.module.hook.misound

import android.annotation.*
import android.content.*
import android.media.*
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.ContextUtils.*
import com.sevtinge.hyperceiler.utils.prefs.*
import de.robv.android.xposed.*
import java.lang.reflect.*


object SoundEffectSwitch : BaseHook() {

    private const val PRE_EFFECT_KEY_NAME = "pre_sound_effect_state"

    private var miSound: Any? = null
    private var dolbyAudioEffect: Any? = null
    private lateinit var audioManager: AudioManager

    private lateinit var miSoundConstructor: Constructor<*>

    private lateinit var dolbyAudioEffectConstructor: Constructor<*>
    private var setBoolParam: Method? = null
    private var getBoolParam: Method? = null

    override fun init() {

        initMiSoundControl()
        initDolbyControl()
        initAudioManagerControl()

        findClass("com.miui.misound.soundid.receiver.BTChangeStaticBroadCastReceiver").hookAfterMethod(
            "onReceive", Context::class.java, Intent::class.java
        ) {
            logDebug("SoundEffectSwitch onReceive")
            onReceive()
        }
    }

    private fun onReceive() {
        try {
            Thread.sleep(400L)
        } catch (e: Exception) {
            logError("sleep interrupted", e)
        }

        val audioDeviceConnState = audioDeviceConnState()
        if (audioDeviceConnState == 0) {
            val preEffectState = getPreEffectState()
            updatePreEffectState(-1)
            logDebug("disconnect | curEffectState:${getEffectState()}, preEffectState:${preEffectState}")
            when (preEffectState) {
                1 -> setMiSoundOn(true)
                2 -> setHarmanOn(true)
                3 -> setDolbyOn(true)
            }
        } else if (audioDeviceConnState == 1) {
            val curEffectState = getEffectState()
            logDebug("connect | curEffectState:${curEffectState}")
            updatePreEffectState(curEffectState)

            setMiSoundOn(false)
            setHarmanOn(false)
            setDolbyOn(false)
        }

        logDebug("processed | curEffectState:${getEffectState()}")
    }

    private fun getPreEffectState(): Int {
        return PrefsUtils.getSharedPrefs(getContext(FLAG_ALL)).getInt(PRE_EFFECT_KEY_NAME, -1)
    }

    private fun updatePreEffectState(state: Int) {
        PrefsUtils.getSharedPrefs(getContext(FLAG_ALL)).edit().putInt(PRE_EFFECT_KEY_NAME, state)
            .apply()
    }

    private fun getEffectState(): Int {
        if (isHarmanOn()) {
            return 2
        } else if (isMiSoundOn()) {
            return 1
        } else if (isDolbyOn()) {
            return 3
        }
        return 0
    }

    private fun setDolbyOn(on: Boolean) {
        try {
            initDolbyAudioEffectIfNeed()
            setBoolParam!!.invoke(dolbyAudioEffect, 0, on)
            val callMethod = XposedHelpers.callMethod(
                dolbyAudioEffect, "setEnabled", on
            )
            logDebug("dolbyAudioEffect#setEnabled return : $callMethod")
        } catch (e: Exception) {
            logError("setDolbyOn fail on:${on}", e)
        }
    }

    private fun setMiSoundOn(on: Boolean) {
        try {
            initMiSoundIfNeed()
            val callMethod = XposedHelpers.callMethod(
                miSound, "setEnabled", on
            )
            logDebug("miSound#setEnabled return : $callMethod")
        } catch (e: Exception) {
            logError("setMiSoundOn fail, on:${on}", e)
        }
    }

    private fun setHarmanOn(on: Boolean) {
        try {
            initMiSoundIfNeed()
            if (on) {
                if (!isMiSoundOn()) {
                    setMiSoundOn(true)
                }
                XposedHelpers.callMethod(miSound, "setScenario", 61)
                audioManager.setParameters("old_volume_curve=false")
            } else {
                XposedHelpers.callMethod(miSound, "setScenario", 60)
                audioManager.setParameters("old_volume_curve=true")
            }
        } catch (e: Exception) {
            logError("setHarmanOn fail, on:${on}", e)
        }
    }

    private fun isMiSoundOn(): Boolean {
        initMiSoundIfNeed()
        return XposedHelpers.callMethod(miSound, "getEnabled") as Boolean
    }

    private fun isHarmanOn(): Boolean {
        initMiSoundIfNeed()
        return audioManager.getParameters("old_volume_curve") == "old_volume_curve=false" && XposedHelpers.callMethod(
            miSound, "getScenario"
        ) as Int == 61
    }

    private fun isDolbyOn(): Boolean {
        initDolbyAudioEffectIfNeed()
        return getBoolParam!!.invoke(dolbyAudioEffect, 0) as Boolean
    }

    private fun initDolbyAudioEffectIfNeed() {
        if (dolbyAudioEffect == null || !(XposedHelpers.callMethod(
                dolbyAudioEffect, "hasControl"
            ) as Boolean)
        ) {
            if (dolbyAudioEffect != null) {
                XposedHelpers.callMethod(dolbyAudioEffect, "release")
            }
            dolbyAudioEffect = dolbyAudioEffectConstructor.newInstance(0, 0)
        }
    }

    private fun initMiSoundIfNeed() {
        if (miSound == null || !(XposedHelpers.callMethod(miSound, "hasControl") as Boolean)) {
            if (miSound != null) {
                XposedHelpers.callMethod(miSound, "release")
            }
            miSound = miSoundConstructor.newInstance(0, 0)
        }
    }

    private fun initDolbyControl() {
        try {
            val dolbyAudioEffectClass = findDolbyAudioEffect()
            dolbyAudioEffectConstructor = dolbyAudioEffectClass.getDeclaredConstructor(
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )

            setBoolParam = try {
                dolbyAudioEffectClass.getDeclaredMethod(
                    "setBoolParam", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                )
            } catch (ignore: Exception) {
                dolbyAudioEffectClass.methodFinder().filterByParamTypes(
                    Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
                ).filterByReturnType(Int::class.javaPrimitiveType as Class<*>).first()
            }
            logDebug("find DolbyAudioEffect#setBoolParam:$setBoolParam")

            getBoolParam = try {
                dolbyAudioEffectClass.getDeclaredMethod("getBoolParam")
            } catch (ignore: Exception) {
                dolbyAudioEffectClass.methodFinder()
                    .filterByParamTypes(Int::class.javaPrimitiveType)
                    .filterByReturnType(Boolean::class.javaPrimitiveType as Class<*>).first()
            }
            logDebug("find DolbyAudioEffect#getBoolParam:$getBoolParam")
        } catch (e: Exception) {
            logError("initDolbyControl fail", e)
        }
    }


    @SuppressLint("PrivateApi")
    private fun initMiSoundControl() {
        try {
            val misoundClass = lpparam.classLoader.loadClass("android.media.audiofx.MiSound")
            miSoundConstructor = misoundClass.getDeclaredConstructor(
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
        } catch (e: Exception) {
            logError("initMiSoundControl fail", e)
        }
    }

    private fun initAudioManagerControl() {
        try {
            audioManager =
                getContext(FLAG_ALL).getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } catch (e: Exception) {
            logError("initAudioManagerControl fail", e)
        }
    }

    private fun findDolbyAudioEffect(): Class<*> {
        return try {
            lpparam.classLoader.loadClass("com.dolby.dax.DolbyAudioEffect")
        } catch (ignore: Exception) {
            DexKit.getDexKitBridge().findClass {
                matcher {
                    superClass = "android.media.audiofx.AudioEffect"
                    usingStrings("setParameter")
                    usingStrings("getParameter")
                    usingStrings("DolbyAudioEffect")
                }
            }.single().getInstance(lpparam.classLoader)
        }
    }

    private fun audioDeviceConnState(): Int {
        return try {
            if (isWiredHeadsetOn() || isUsbHeadsetOn() || isBtA2dpInUse()) 1 else 0
        } catch (e: Exception) {
            logError("get audioDeviceConnState fail", e)
            -1
        }
    }

    /**
     * usb 耳机
     */
    private fun isUsbHeadsetOn(): Boolean {
        return XposedHelpers.callStaticMethod(
            findClass("android.media.AudioSystem"), "getDeviceConnectionState", 67108864, ""
        ) as Int == 1
    }

    /**
     * 有线耳机
     */
    private fun isWiredHeadsetOn(): Boolean {
        return XposedHelpers.callMethod(
            getContext(FLAG_ALL).getSystemService(Context.AUDIO_SERVICE), "isWiredHeadsetOn"
        ) as Boolean
    }

    /**
     * 蓝牙耳机
     */
    private fun isBtA2dpInUse(): Boolean {
        val isBluetoothA2dpOn1 = XposedHelpers.callMethod(
            getContext(FLAG_ALL).getSystemService(Context.AUDIO_SERVICE), "isBluetoothA2dpOn"
        ) as Boolean

        val bluetoothAdapter = XposedHelpers.callStaticMethod(
            findClass("android.bluetooth.BluetoothAdapter"), "getDefaultAdapter"
        )

        val isBluetoothA2dpOn2 = XposedHelpers.callMethod(
            bluetoothAdapter, "getProfileConnectionState", 2
        ) as Int == 2

        return isBluetoothA2dpOn1 && isBluetoothA2dpOn2
    }

    private fun logError(msg: String, e: Exception) {
        logE(TAG, lpparam.packageName, msg, e)
        XposedBridge.log(e)
    }

    private fun logDebug(msg: String) {
        logD(TAG, lpparam.packageName, msg)
    }
}