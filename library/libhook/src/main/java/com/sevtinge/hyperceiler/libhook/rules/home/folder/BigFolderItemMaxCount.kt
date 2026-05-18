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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.folder

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs

class BigFolderItemMaxCount : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initOS3Hook() {
        hook("com.miui.home.folder.FolderIcon2x2", "com.miui.home.folder.FolderIconPreviewContainer2X2_")
    }

    override fun initBase() {
        hook("com.miui.home.launcher.folder.FolderIcon2x2", "com.miui.home.launcher.folder.FolderIconPreviewContainer2X2_")
    }

    private fun hook(clazz: String, clazz2: String) {
        findClass(clazz).findMethod {
            name("createOrRemoveView")
        }.createBeforeHook { param ->
            val thisObject = param.thisObject
            val info = thisObject.getObjectFieldAs<Any>("mInfo")
            val container = thisObject.callMethod("getMPreviewContainer") as Any

            val childCount1: Int = info.callMethod("count") as Int
            val childCount2: Int = container.callMethod("getMRealPvChildCount") as Int
            if (childCount1 == childCount2) {
                return@createBeforeHook
            }

            val num: Int = Character.getNumericValue(container.javaClass.simpleName.last())
            if (Integer.compare(childCount2, num + 3) >= 0) {
                return@createBeforeHook
            }

            container.callMethod(
                "setMItemsMaxCount",
                if (childCount1 <= num) {
                    num
                } else {
                    num + 3
                }
            )
        }

        findClass(clazz).findMethod {
            name("addItemOnclickListener")
        }.createBeforeHook { param ->
            val container = param.thisObject.callMethod("getMPreviewContainer") as Any
            val childCount: Int = container.callMethod("getMRealPvChildCount") as Int

            val num: Int = Character.getNumericValue(container.javaClass.simpleName.last())
            param.thisObject.callMethod(
                "setMLargeIconNum",
                if (childCount <= num) {
                    num
                } else {
                    num - 1
                }
            )
        }

        val hookFolderIconPreviewContainer = { num: Int ->
            findClass(clazz2 + num).findMethod {
                name("preSetup2x2")
            }.createBeforeHook { param ->
                val container = param.thisObject
                val childCount: Int = container.callMethod("getMRealPvChildCount") as Int

                container.callMethod(
                    "setMLargeIconNum",
                    if (childCount <= num) {
                        num
                    } else {
                        num - 1
                    }
                )
            }
        }

        hookFolderIconPreviewContainer(4)
        hookFolderIconPreviewContainer(9)
    }
}
