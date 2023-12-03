package com.sevtinge.hyperceiler.module.hook.mediaeditor

import android.os.Build
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.api.LazyClass.AndroidBuildCls


object FilterManagerAll : BaseHook() {
    private lateinit var device: String
    private val methodResult by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("wayne")
            }
        }.filter { it.isMethod }.map { it.getMethodInstance(safeClassLoader) }.toTypedArray().firstOrNull()
    }

    override fun init() {
        methodResult?.createHook {
            before {
                if (!this@FilterManagerAll::device.isInitialized) {
                    device = Build.DEVICE
                }
                setStaticObject(AndroidBuildCls, "DEVICE", "wayne")
            }
            after {
                setStaticObject(AndroidBuildCls, "DEVICE", device)
            }
        }
    }
}
