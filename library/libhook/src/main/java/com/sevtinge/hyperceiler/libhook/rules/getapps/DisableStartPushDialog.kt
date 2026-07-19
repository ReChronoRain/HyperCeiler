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
package com.sevtinge.hyperceiler.libhook.rules.getapps

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook


object DisableStartPushDialog : BaseHook() {
    override fun init() {
        // 禁用开启推送弹窗
        runCatching {
            loadClass("com.xiaomi.market.data.NotificationRecallController").apply {
                findMethod {
                    name("tryShowDialog")
                }.createHook {
                    interrupt()
                }

                findMethod {
                    name("checkAndTryShowDialog")
                }.createHook {
                    returnConstant(false)
                }
            }
        }.onFailure {
            loadClass("com.xiaomi.market.ui.UpdateListFragment")
                .findMethod {
                    name("tryShowDialog")
                }.createHook {
                    interrupt()
                }
            loadClass("com.xiaomi.market.ui.update.UpdatePushDialogManager")
                .findMethod {
                    name("tryShowDialog")
                }.createHook {
                    interrupt()
                }
        }
    }
}
