package com.sevtinge.hyperceiler.module.hook.screenrecorder

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import de.robv.android.xposed.XposedHelpers

@SuppressLint("StaticFieldLeak")
object UnlockMoreVolumeFromNew : BaseHook() {
    private val getClass by lazy {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("support_a2dp_inner_record")
            }
        }.firstOrNull()?.getInstance(safeClassLoader)
    }

    override fun init() {
        val fieldData = dexKitBridge.findField {
            matcher {
                getClass?.let { declaredClass(it) }
                type = "boolean"
            }
        }.map { it.getFieldInstance(EzXHelper.classLoader) }.toList()

        findAndHookConstructor(getClass, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                for (i in fieldData) {
                    XposedHelpers.setObjectField(param.thisObject, i.name, true)
                }
            } })
    }
}
