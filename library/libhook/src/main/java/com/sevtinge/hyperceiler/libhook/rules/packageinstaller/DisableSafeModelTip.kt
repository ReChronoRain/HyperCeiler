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
package com.sevtinge.hyperceiler.libhook.rules.packageinstaller

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setBooleanField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Field
import java.lang.reflect.Method

object DisableSafeModelTip : BaseHook() {
    override fun useDexKit() = true
    private lateinit var disableSafeModelTipMethod: Method
    private lateinit var recyclerViewField: Field

    override fun initDexKit(): Boolean {
        disableSafeModelTipMethod = requiredMember("DisableSafeModelTip") {
            it.findMethod {
                matcher {
                    usingEqStrings($$"android.provider.MiuiSettings$Ad")
                }
            }.singleOrNull()
        }
        recyclerViewField = requiredMember("RecyclerView") {
            it.findClass {
                matcher {
                    className = "com.miui.packageInstaller.ui.listcomponets.SafeModeTipViewObject"
                }
            }.single().superClass?.findField {
                matcher {
                    type = "boolean"
                }
            }?.single()
        }
        return true
    }

    override fun init() {
        disableSafeModelTipMethod.createHook {
            replace { false }
        }

        recyclerViewField.declaringClass.findMethod { name("a") }.createAfterHook {
            it.thisObject.setBooleanField(recyclerViewField.name, false)
        }
    }
}
