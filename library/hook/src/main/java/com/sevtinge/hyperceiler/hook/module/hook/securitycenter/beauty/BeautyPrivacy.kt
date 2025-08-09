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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.beauty

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHooks
import java.lang.reflect.Method

object BeautyPrivacy : BaseHook() {
    private val R0 by lazy<Method> {
        DexKit.findMember("BeautyPrivacy") {
            it.findMethod {
                matcher {
                    usingEqStrings("persist.sys.privacy_camera")
                }
            }.single()
        }
    }

    private val invokeMethod by lazy<List<Method>> {
        DexKit.findMemberList("BeautyPrivacyList") {
            it.findClass {
                matcher {
                    usingEqStrings("persist.sys.privacy_camera")
                }
            }.findMethod {
                matcher {
                    paramTypes = emptyList()
                    returnType = "boolean"
                    addInvoke {
                        declaredClass = R0.declaringClass.name
                        returnType = R0.returnType.name
                        paramTypes = listOf(R0.parameterTypes[0].name)
                    }
                }
            }
        }
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
