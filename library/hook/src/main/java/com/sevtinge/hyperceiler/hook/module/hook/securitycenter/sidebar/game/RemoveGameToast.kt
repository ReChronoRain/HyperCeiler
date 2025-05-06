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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.Method

// by YifePlayte
object RemoveGameToast : BaseHook() {
    private val removeMethod1 by lazy<Method> {
        DexKit.findMember("gameToast1") {
            it.findMethod {
                matcher {
                    usingStrings = listOf("showWildModeToastView: ")
                }
            }.single()
        }
    }

    private val removeMethod2 by lazy<Method> {
        DexKit.findMember("gameToast2") {
            it.findMethod {
                matcher {
                    usingStrings = listOf("cancel game toast , isCanceled : ")
                }
            }.single()
        }
    }

    override fun init() {
        removeMethod1.createHook {
            returnConstant(null)
        }

        removeMethod2.createHook {
            returnConstant(null)
        }
    }
}