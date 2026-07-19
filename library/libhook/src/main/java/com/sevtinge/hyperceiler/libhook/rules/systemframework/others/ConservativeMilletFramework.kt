/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object ConservativeMilletFramework : BaseHook() {

    override fun init() {
        val aurogonImmobulusModeClass =
            findClassIfExists("com.miui.server.greeze.AurogonImmobulusMode")

        val domesticPolicyManagerClass =
            findClassIfExists("com.miui.server.greeze.DomesticPolicyManager")

        hookBefore(
            aurogonImmobulusModeClass,
            "isNeedRestictNetworkPolicy",
            false,
            Int::class.java
        )
        hookBefore(domesticPolicyManagerClass, "isAllowBroadcast", true)
        hookBefore(domesticPolicyManagerClass, "isRestrictBroadcast", true, Int::class.java)
        hookBefore(domesticPolicyManagerClass, "isJobRestrict", true, Int::class.java)
        hookBefore(domesticPolicyManagerClass, "deferBroadcast", false, String::class.java)
        hookBefore(domesticPolicyManagerClass, "isRestrictNet", false, Int::class.java)
    }

    private fun hookBefore(
        clazz: Class<*>?,
        methodName: String,
        result: Any,
        vararg parameterTypes: Class<*>
    ) {
        if (clazz == null) return

        try {
            clazz.findMethod {
                name(methodName)
                parameterTypes(*parameterTypes)
            }.createBeforeHook {
                it.result = result
            }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in $methodName: ", e)
        }
    }
}
