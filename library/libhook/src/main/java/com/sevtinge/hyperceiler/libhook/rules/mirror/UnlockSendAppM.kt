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
package com.sevtinge.hyperceiler.libhook.rules.mirror

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object UnlockSendAppM : BaseHook() {
    private val method1 by lazy {
        DexKit.findMember("subScreen") {bridge ->
            bridge.findMethod {
                matcher {
                    addUsingString("support_all_app_sub_screen", StringMatchType.Equals)
                    returnType = "boolean"
                }
            }.single()
        } as? Method
    }

    private val class2 by lazy<List<Class<*>>> {
        DexKit.findMemberList("relayAppMessage") {bridge ->
            bridge.findClass {
                matcher {
                    addUsingString("RelayAppMessage{type=")
                    addInterface("com.xiaomi.mirror.message.Message")
                }
            }
        }
    }

    override fun init() {
        val relayAppClass = findClassIfExists(
            $$"com.xiaomi.mirror.message.proto.RelayApp$RelayApplication"
        ) ?: return

        relayAppClass.apply {
            beforeHookMethod("getIsHideIcon") {
                returnConstant(false)
            }
            beforeHookMethod("getSupportHandOff") {
                returnConstant(true)
            }
            beforeHookMethod("getSupportSubScreen") {
                returnConstant(true)
            }
        }

        method1?.createHook {
            returnConstant(true)
        }

        class2.forEach { clazz ->
            // 找所有 boolean 字段，按名称排序，取第 2 个
            val targetFieldName = clazz.declaredFields
                .filter { it.type == Boolean::class.javaPrimitiveType }
                .sortedBy { it.name }
                .getOrNull(1)?.name ?: return

            val factoryMethods = clazz.declaredMethods
                .filter { it.returnType == clazz }

            factoryMethods.forEach {
                it.createAfterHook { param ->
                    param.result?.setObjectField(targetFieldName, false)
                }
            }
        }
    }
}
