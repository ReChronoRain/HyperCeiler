package com.sevtinge.hyperceiler.module.hook.screenrecorder

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Modifier

object UnlockMoreVolumeFromNew : BaseHook() {
    private val getClass by lazy {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("ro.vendor.audio.screenrecorder.bothrecor")
            }
        }.map { it.getInstance(EzXHelper.classLoader) }.first()
    }

    private val getObject by lazy {
        dexKitBridge.findField {
            matcher {
                declaredClass { getClass }
                modifiers = Modifier.PRIVATE
                type = "boolean"
            }
        }.map { it.getFieldInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        XposedLogUtils.logI("hook class $getClass")
        for (i in getObject) {
            XposedHelpers.setStaticBooleanField(getClass, i.name, true)
            logD("hook ${i.name} true")
        }
    }
}
