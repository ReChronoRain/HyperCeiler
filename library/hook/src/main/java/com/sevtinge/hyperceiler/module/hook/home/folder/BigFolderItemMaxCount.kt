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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.home.folder

import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.module.base.BaseHook

class BigFolderItemMaxCount : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.miui.home.launcher.folder.FolderIcon2x2", "createOrRemoveView",
            object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
                override fun before(param: MethodHookParam) {
                    val thisObject = param.thisObject
                    val info = thisObject.getObjectFieldAs<Any>("mInfo")
                    val container = thisObject.callMethodAs<Any>("getMPreviewContainer")

                    val childCount1 = info.callMethodAs<Int>("count")
                    val childCount2 = container.callMethodAs<Int>("getMRealPvChildCount")
                    if (childCount1 == childCount2) {
                        return
                    }

                    val num = Character.getNumericValue(container::class.java.simpleName.last())
                    if (childCount2 - num >= 3) {
                        return
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
            }
        )

        findAndHookMethod(
            "com.miui.home.launcher.folder.FolderIcon2x2", "addItemOnclickListener",
            object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
                override fun before(param: MethodHookParam) {
                    val container = param.thisObject.callMethodAs<Any>("getMPreviewContainer")
                    val childCount = container.callMethodAs<Int>("getMRealPvChildCount")

                    val num = Character.getNumericValue(container::class.java.simpleName.last())
                    param.thisObject.callMethod(
                        "setMLargeIconNum",
                        if (childCount <= num) {
                            num
                        } else {
                            num - 1
                        }
                    )
                }
            }
        )

        val hookFolderIconPreviewContainer = { num: Int ->
            findAndHookMethod(
                "com.miui.home.launcher.folder.FolderIconPreviewContainer2X2_$num", "preSetup2x2",
                object : com.sevtinge.hyperceiler.module.base.tool.HookTool.MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val container = param.thisObject
                        val childCount = container.callMethodAs<Int>("getMRealPvChildCount")

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
            )
        }

        hookFolderIconPreviewContainer(4)
        hookFolderIconPreviewContainer(9)
    }
}
