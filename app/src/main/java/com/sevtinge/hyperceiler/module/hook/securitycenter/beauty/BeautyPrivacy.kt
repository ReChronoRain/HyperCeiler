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
package com.sevtinge.hyperceiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.addUsingStringsEquals
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toElementList
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod

object BeautyPrivacy : BaseHook() {
    private val R0 by lazy {
        DexKit.getDexKitBridge("BeautyPrivacy") {
            it.findMethod {
                matcher {
                    addUsingStringsEquals("persist.sys.privacy_camera")
                }
            }.single().getMethodInstance(EzXHelper.safeClassLoader)
        }.toMethod()
    }

    private val invokeMethod by lazy {
        DexKit.getDexKitBridgeList("BeautyPrivacyList") {
            it.findMethod {
                matcher {
                    declaredClass {
                        addUsingStringsEquals("persist.sys.privacy_camera")
                    }
                    paramTypes = emptyList()
                    returnType = "boolean"
                    addInvoke {
                        declaredClass {
                            addUsingStringsEquals("persist.sys.privacy_camera")
                        }
                        returnType = R0.returnType.name
                        paramTypes = listOf(R0.parameterTypes[0].name)
                    }
                }
            }.toElementList(EzXHelper.classLoader)
        }.toMethodList()
    }

    override fun init() {
        R0.createHook {
            before {
                it.args[0] = true
            }
        }

        invokeMethod.createHooks {
            returnConstant(true)
        }
    }
}
