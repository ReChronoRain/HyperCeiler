package com.sevtinge.hyperceiler.module.hook.updater

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit.dexKitBridge
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.XposedBridge.*
import org.luckypray.dexkit.query.enums.*

object AutoUpdateDialog : BaseHook() {
    private val find1 by lazy {
        dexKitBridge.findMethod {
            matcher {
                addCall {
                    addUsingString("isShowAutoSetDialog", StringMatchType.Contains)
                }
                paramTypes("boolean", "boolean")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    private val find2 by lazy {
        dexKitBridge.findMethod {
            matcher {
                addCall {
                    addUsingString("isShowMobileDownloadDialog", StringMatchType.Contains)
                }
                paramTypes("long", "int")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    override fun init() {
        logD(TAG, lpparam.packageName, "get find1 is $find1")
        logD(TAG, lpparam.packageName, "get find2 is $find2")
        setOf(find1, find2).forEach {
            it.replaceMethod { 0 }
        }
    }
}