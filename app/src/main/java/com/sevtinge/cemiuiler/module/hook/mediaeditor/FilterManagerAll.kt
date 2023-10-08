package com.sevtinge.cemiuiler.module.hook.mediaeditor

import android.os.Build
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.api.LazyClass.AndroidBuildCls
import org.luckypray.dexkit.query.enums.StringMatchType


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
        /*val result = mMediaEditorResultMethodsMap["FilterManager"]!!
        val methodResult = result
        MethodFinder.fromArray(methodResult).first {
            returnType == List::class.java || (parameterCount == 1 && parameterTypes[0] == Bundle::class.java)
        }*/
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
