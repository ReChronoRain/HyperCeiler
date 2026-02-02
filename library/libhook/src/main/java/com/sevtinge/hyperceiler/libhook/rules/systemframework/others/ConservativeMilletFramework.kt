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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog

object ConservativeMilletFramework : BaseHook() {

    override fun init() {
        val aurogonImmobulusModeClass =
            findClassIfExists("com.miui.server.greeze.AurogonImmobulusMode")

        val domesticPolicyManagerClass =
            findClassIfExists("com.miui.server.greeze.DomesticPolicyManager")

        try {
            aurogonImmobulusModeClass
                .beforeHookMethod(
                    "isNeedRestictNetworkPolicy",
                    Int::class.java
                ) {
                    it.result = false
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in isNeedRestictNetworkPolicy: ", e)
        }

        try {
            domesticPolicyManagerClass
                .beforeHookMethod(
                    "isAllowBroadcast"
                ) {
                    it.result = true
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in isAllowBroadcast: ", e)
        }

        try {
            domesticPolicyManagerClass
                .beforeHookMethod(
                    "isRestrictBroadcast",
                    Int::class.java
                ) {
                    it.result = true
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in isRestrictBroadcast: ", e)
        }

        try {
            domesticPolicyManagerClass
                .beforeHookMethod(
                    "isJobRestrict",
                    Int::class.java
                ) {
                    it.result = true
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in isJobRestrict: ", e)
        }

        try {
            domesticPolicyManagerClass
                .beforeHookMethod(
                    "deferBroadcast",
                    String::class.java
                ) {
                    it.result = false
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in deferBroadcast: ", e)
        }

        try {
            domesticPolicyManagerClass
                .beforeHookMethod(
                    "isRestrictNet",
                    Int::class.java
                ) {
                    it.result = false
                }
        } catch (e: Exception) {
            XposedLog.w(TAG, packageName, "Hook Failed in isRestrictNet: ", e)
        }
    }
}
