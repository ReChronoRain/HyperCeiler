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
package com.sevtinge.hyperceiler.module.hook.various

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod

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
            DexKit.getDexKitBridge("dexKitSuperClipboardRo") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("ro.miui.support_super_clipboard")
                        returnType = "boolean"
                    }
                }.singleOrNull()?.getMethodInstance(safeClassLoader)
            }.toMethod()
        }

        val sys by lazy {
            DexKit.getDexKitBridge("dexKitSuperClipboardSys") {
                it.findMethod {
                    matcher {
                        addUsingStringsEquals("persist.sys.support_super_clipboard")
                        returnType = "boolean"
                    }
                }.singleOrNull()?.getMethodInstance(safeClassLoader)
            }.toMethod()
        }

        setOf(ro, sys).toSet().createHooks {
            returnConstant(switch)
        }
    }
}
