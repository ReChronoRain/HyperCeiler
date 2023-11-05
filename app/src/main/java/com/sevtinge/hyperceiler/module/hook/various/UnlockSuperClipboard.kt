package com.sevtinge.hyperceiler.module.hook.various

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockSuperClipboard : BaseHook() {
    // by StarVoyager
    override fun init() {
        when (EzXHelper.hostPackageName) {
            "com.miui.gallery" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_gallery_int", 0)) {
                    1 -> methodSuperClipboard("com.miui.gallery.util.MiscUtil", true)
                    2 -> methodSuperClipboard("com.miui.gallery.util.MiscUtil", false)
                }
            }

            "com.android.fileexplorer" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_fileexplorer_int", 0)) {
                    1 -> methodSuperClipboard("com.android.fileexplorer.model.Util", true)
                    2 -> methodSuperClipboard("com.android.fileexplorer.model.Util", false)
                }
            }

            "com.miui.screenshot" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_screenshot_int", 0)) {
                    1 -> dexKitSuperClipboard(true)
                    2 -> dexKitSuperClipboard(false)
                }
            }

            "com.android.browser" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_browser_int", 0)) {
                    1 -> dexKitSuperClipboard(true)
                    2 -> dexKitSuperClipboard(false)
                }
            }

            "com.android.mms" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_mms_int", 0)) {
                    1 -> dexKitSuperClipboard(true)
                    2 -> dexKitSuperClipboard(false)
                }
            }

            "com.miui.notes" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_notes_int", 0)) {
                    1 -> methodSuperClipboard("com.miui.common.tool.Utils", true)
                    2 -> methodSuperClipboard("com.miui.common.tool.Utils", false)
                }
            }

            "com.miui.creation" -> {
                when (mPrefsMap.getStringAsInt("various_super_clipboard_creation_int", 0)) {
                    1 -> methodSuperClipboard("com.miui.creation.common.tools.ClipUtils", true)
                    2 -> methodSuperClipboard("com.miui.creation.common.tools.ClipUtils", false)
                }
            }
        }
    }

    private fun methodSuperClipboard(clsName: String, switch: Boolean) {
        loadClass(clsName).methodFinder()
            .filterByName("isSupportSuperClipboard")
            .first().createHook {
                returnConstant(switch)
            }
    }

    private fun dexKitSuperClipboard(switch: Boolean) {
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
            returnConstant(switch)
        }
    }
}
