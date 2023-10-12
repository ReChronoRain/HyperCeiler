package com.sevtinge.cemiuiler.module.hook.various

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit

object UnlockSuperClipboard : BaseHook() {
    // by StarVoyager
    override fun init() {
        when (EzXHelper.hostPackageName) {
            "com.miui.gallery" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_gallery")) {
                    methodSuperClipboard("com.miui.gallery.util.MiscUtil")
                }
            }

            "com.android.fileexplorer" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_fileexplorer")) {
                    methodSuperClipboard("com.android.fileexplorer.model.Util")
                }
            }

            "com.miui.screenshot" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_screenshot")) {
                    dexKitSuperClipboard()
                }
            }

            "com.android.browser" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_browser")) {
                    dexKitSuperClipboard()
                }
            }

            "com.android.mms" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_mms")) {
                    dexKitSuperClipboard()
                }
            }

            "com.miui.notes" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_notes")) {
                    methodSuperClipboard("com.miui.common.tool.Utils")
                }
            }

            "com.miui.creation" -> {
                if (mPrefsMap.getBoolean("various_super_clipboard_creation")) {
                    methodSuperClipboard("com.miui.creation.common.tools.ClipUtils")
                }
            }
        }
    }

    private fun methodSuperClipboard(clsName: String) {
        loadClass(clsName).methodFinder()
            .filterByName("isSupportSuperClipboard")
            .first().createHook {
                returnConstant(true)
            }
    }

    private fun dexKitSuperClipboard() {
        initDexKit(lpparam)
        val ro by lazy {
            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals("ro.miui.support_super_clipboard")
                    returnType = "boolean"
                }
            }.firstOrNull()?.getMethodInstance(safeClassLoader)
        }

        val sys by lazy {
            dexKitBridge.findMethod {
                matcher {
                    addUsingStringsEquals("persist.sys.support_super_clipboard")
                    returnType = "boolean"
                }
            }.firstOrNull()?.getMethodInstance(safeClassLoader)
        }

        setOf(ro, sys).filterNotNull().createHooks {
            returnConstant(true)
        }
        closeDexKit()
    }
}
