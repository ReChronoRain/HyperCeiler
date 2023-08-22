package com.sevtinge.cemiuiler.module.various

import com.github.kyuubiran.ezxhelper.ClassUtils
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.safeDexKitBridge
import io.luckypray.dexkit.enums.MatchType

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
        ClassUtils.loadClass(clsName).methodFinder()
            .filterByName("isSupportSuperClipboard")
            .first().createHook {
                returnConstant(true)
            }
    }

    private fun dexKitSuperClipboard() {
        try {
            safeDexKitBridge.findMethodUsingString {
                usingString = "persist.sys.support_super_clipboard"
                matchType = MatchType.FULL
                methodReturnType = "boolean"
            }.firstOrNull()?.getMethodInstance(EzXHelper.safeClassLoader)?.createHook {
                returnConstant(true)
            }
        } catch (t: Throwable) {
            safeDexKitBridge.findMethodUsingString {
                usingString = "ro.miui.support_super_clipboard"
                matchType = MatchType.FULL
                methodReturnType = "boolean"
            }.firstOrNull()?.getMethodInstance(EzXHelper.safeClassLoader)?.createHook {
                returnConstant(true)
            }
        }
    }
}
