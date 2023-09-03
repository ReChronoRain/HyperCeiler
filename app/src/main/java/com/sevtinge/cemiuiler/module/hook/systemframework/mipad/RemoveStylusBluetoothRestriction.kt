package com.sevtinge.cemiuiler.module.hook.systemframework.mipad

import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.sevtinge.cemiuiler.module.base.BaseHook

object RemoveStylusBluetoothRestriction : BaseHook() {
    override fun init() {
        val clazzMiuiStylusDeviceListener =
            loadClass("com.miui.server.input.stylus.MiuiStylusDeviceListener")
        clazzMiuiStylusDeviceListener.declaredConstructors.createHooks {
            after {
                setTouchModeStylusEnable()
            }
        }
        clazzMiuiStylusDeviceListener.declaredMethods.createHooks {
            replace {
                setTouchModeStylusEnable()
            }
        }
    }

    private fun setTouchModeStylusEnable() {
        val driverVersion =
            mPrefsMap.getStringAsInt("mipad_input_bluetooth_version", 2)
        val flag: Int = 0x10 or driverVersion
        val instanceITouchFeature =
            invokeStaticMethodBestMatch(
                loadClass("miui.util.ITouchFeature"),
                "getInstance"
            )!!
        ObjectUtils.invokeMethodBestMatch(
            instanceITouchFeature,
            "setTouchMode",
            null,
            0, 20, flag
        )
    }
}
