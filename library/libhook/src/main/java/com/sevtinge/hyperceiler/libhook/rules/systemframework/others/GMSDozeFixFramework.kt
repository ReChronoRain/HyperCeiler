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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hookAllConstructors
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog

object GMSDozeFixFramework : BaseHook() {

    override fun init() {
        val greezeManagerServiceClass =
            findClassIfExists("com.miui.server.greeze.GreezeManagerService")

        val listAppsManagerClass = findClassIfExists("com.miui.server.greeze.power.ListAppsManager")

        val awareResourceControlClass =
            findClassIfExists("com.miui.server.greeze.power.AwareResourceControl")

        try {
            greezeManagerServiceClass
                .beforeHookMethod(
                    "isAllowBroadcast",
                    Int::class.java,
                    String::class.java,
                    Int::class.java,
                    String::class.java,
                    String::class.java
                ) {
                    val calleePkgName = it.args[3] as String
                    if (calleePkgName.contains("com.google.android.gms")) it.result = true
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in isAllowBroadcast: ", e)
        }

        try {
            greezeManagerServiceClass
                .beforeHookMethod(
                    "triggerGMSLimitAction",
                    Boolean::class.java
                ) {
                    it.args[0] = true
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in triggerGMSLimitAction: ", e)
        }

        try {
            listAppsManagerClass
                .hookAllConstructors {
                    before {
                        @Suppress("UNCHECKED_CAST")
                        val targetFields = setOf(
                            "mSystemBlackList",
                            "SLEEP_MODE_LIST",
                            "mMiDataWhiteList",
                            "mDataWhiteList"
                        )

                        listAppsManagerClass.declaredFields
                            .map { field -> field.name }
                            .filter { name -> name in targetFields }
                            .forEach { fieldName ->
                                val list = getObjectField(
                                    it.thisObject,
                                    fieldName
                                ) as MutableList<String>

                                when (fieldName) {
                                    "mSystemBlackList" -> list.remove("com.google.android.gms")
                                    else -> list.add("com.google.android.gms")
                                }

                                setObjectField(it.thisObject, fieldName, list)
                            }
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in listAppsManagerClass constructor", e )
        }

        try {
            listAppsManagerClass
                .beforeHookMethod(
                    "isInCloudWhiteList",
                    String::class.java
                ) {
                    val name = it.args[0] as String
                    if (name.contains("com.google.android.gms")) it.result = true
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in isInCloudWhiteList", e)
        }

        try {
            listAppsManagerClass
                .beforeHookMethod(
                    "isInWhiteList",
                    String::class.java
                ) {
                    val name = it.args[0] as String
                    if (name.contains("com.google.android.gms")) it.result = true
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in isInWhiteList", e)
        }

        try {
            awareResourceControlClass
                .beforeHookMethod(
                    "isNetworkDisableAble",
                    String::class.java
                ) {
                    it.result = false
                }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in isNetworkDisableAble", e)
        }

    }

}
