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
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.app.admin.DevicePolicyManager
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField

object DisableDeviceManagedNew {
    @JvmStatic
    fun initDisableDeviceManaged(classLoader: ClassLoader) {
        val securityController by lazy {
            loadClass("miui.systemui.controlcenter.policy.SecurityController", classLoader)
        }

        Constructors.find(securityController)
            .filterByParamCount(5)
            .first().createBeforeHook {
                it.thisObject.setObjectField("hasCACerts", null)
            }

        DevicePolicyManager::class.java.findMethod { name("isDeviceManaged") }.createHook {
                returnConstant(false)
            }

        securityController.findMethod { name("isDeviceManaged") }.createHook {
                returnConstant(false)
            }

        securityController.findMethod { name("hasCACertInCurrentUser") }.createHook {
                returnConstant(false)
            }

        securityController.findMethod { name("hasCACertInWorkProfile") }.createHook {
                returnConstant(false)
            }
    }
}
