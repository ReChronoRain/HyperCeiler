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
package com.sevtinge.hyperceiler.hook.module.rules.packageinstaller

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import com.sevtinge.hyperceiler.hook.utils.setBooleanField
import java.lang.reflect.Field
import java.lang.reflect.Method

object DisableSafeModelTip : BaseHook() {
    override fun init() {
        DexKit.findMember<Method>("DisableSafeModelTip") {
            it.findMethod {
                matcher {
                    usingEqStrings($$"android.provider.MiuiSettings$Ad")
                }
            }.singleOrNull()
        }.replaceMethod {
            false
        }

        val field = DexKit.findMember("RecyclerView") {
            it.findClass {
                matcher {
                    className = "com.miui.packageInstaller.ui.listcomponets.SafeModeTipViewObject"
                }
            }.single().superClass?.findField {
                matcher {
                    type = "boolean"
                }
            }?.single()
        } as Field

        field.declaringClass.hookAfterMethod("a") {
            it.thisObject.setBooleanField(field.name, false)
        }
    }
}
