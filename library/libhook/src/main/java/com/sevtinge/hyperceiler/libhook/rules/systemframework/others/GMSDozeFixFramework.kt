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

import android.content.Intent
import android.content.pm.ResolveInfo
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.chainMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.deoptimizeConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.deoptimizeMethod

/**
 * Source:
 * https://github.com/Howard20181/HyperOS_FCM_Live/blob/main/HyperFCMLive/src/main/java/io/github/howard20181/hyperos/fcmlive/Hooker.java
 */
object GMSDozeFixFramework : BaseHook() {
    private val cnDeferBroadcast = setOf(
        "com.google.android.intent.action.GCM_RECONNECT",
        "com.google.android.gcm.DISCONNECTED",
        "com.google.android.gcm.CONNECTED",
        "com.google.android.gms.gcm.HEARTBEAT_ALARM"
    )
    private val gmsPackageName = "com.google.android.gms"
    private val gmsPersistentProcessName = "com.google.android.gms.persistent"
    private val actionRemoteIntent = "com.google.android.c2dm.intent.RECEIVE"

    override fun init() {
        val greezeManagerServiceClass =
            findClassIfExists("com.miui.server.greeze.GreezeManagerService")

        val domesticPolicyManagerClass =
            findClassIfExists("com.miui.server.greeze.DomesticPolicyManager")

        val listAppsManagerClass = findClassIfExists("com.miui.server.greeze.power.ListAppsManager")

        val awareResourceControlClass =
            findClassIfExists("com.miui.server.greeze.power.AwareResourceControl")

        val broadcastQueueModernStubImplClass =
            findClassIfExists("com.android.server.am.BroadcastQueueModernStubImpl")

        val broadcastQueueClass = findClassIfExists("com.android.server.am.BroadcastQueue")
        val broadcastRecordClass = findClassIfExists("com.android.server.am.BroadcastRecord")
        val processPolicyClass = findClassIfExists("com.android.server.am.ProcessPolicy")

        greezeManagerServiceClass?.let { clazz ->
            try {
                clazz.chainMethod(
                    "isAllowBroadcast",
                    Int::class.java,
                    String::class.java,
                    Int::class.java,
                    String::class.java,
                    String::class.java
                ) {
                    val calleePkgName = getArg(3) as? String ?: return@chainMethod proceed()
                    val action = getArg(4) as? String ?: return@chainMethod proceed()
                    if (calleePkgName.contains(gmsPackageName) &&
                        (action == actionRemoteIntent || action in cnDeferBroadcast)
                    ) {
                        return@chainMethod true
                    }
                    proceed()
                }
                clazz.getDeclaredMethod(
                    "isAllowBroadcast",
                    Int::class.java,
                    String::class.java,
                    Int::class.java,
                    String::class.java,
                    String::class.java
                ).deoptimizeMethod()
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in isAllowBroadcast: ", e)
            }

            try {
                clazz.chainMethod(
                    "deferBroadcastForMiui",
                    String::class.java
                ) {
                    val action = getArg(0) as? String ?: return@chainMethod proceed()
                    if (action in cnDeferBroadcast) {
                        return@chainMethod false
                    }
                    proceed()
                }
                clazz.getDeclaredMethod(
                    "deferBroadcastForMiui",
                    String::class.java
                ).deoptimizeMethod()
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in deferBroadcastForMiui: ", e)
            }

            try {
                clazz.chainMethod(
                    "triggerGMSLimitAction",
                    Boolean::class.java
                ) {
                    val methodArgs = args.toMutableList()
                    if (methodArgs.isNotEmpty()) {
                        methodArgs[0] = false
                    }
                    proceed(methodArgs.toTypedArray())
                }
                clazz.getDeclaredMethod(
                    "triggerGMSLimitAction",
                    Boolean::class.java
                ).deoptimizeMethod()
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in triggerGMSLimitAction: ", e)
            }
        }

        domesticPolicyManagerClass?.let { clazz ->
            try {
                clazz.chainMethod(
                    "deferBroadcast",
                    String::class.java
                ) {
                    false
                }
                clazz.getDeclaredMethod(
                    "deferBroadcast",
                    String::class.java
                ).deoptimizeMethod()
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in deferBroadcast: ", e)
            }
        }

        listAppsManagerClass?.let { clazz ->
            try {
                chainAllConstructors(clazz) { chain ->
                    val result = chain.proceed()
                    try {
                        val thisObject = chain.thisObject ?: return@chainAllConstructors result
                        val blackList = getObjectField(thisObject, "mSystemBlackList")
                            as? MutableCollection<Any?>
                            ?: return@chainAllConstructors result
                        blackList.remove(gmsPackageName)
                    } catch (e: Exception) {
                        XposedLog.e(TAG, packageName, "Failed to modify mSystemBlackList", e)
                    }
                    result
                }
                clazz.declaredConstructors.forEach { constructor ->
                    constructor.deoptimizeConstructor()
                }
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in ListAppsManager constructor", e)
            }
        }

        broadcastQueueModernStubImplClass?.let { clazz ->
            if (broadcastQueueClass == null || broadcastRecordClass == null) return@let
            try {
                clazz.chainMethod(
                    "checkApplicationAutoStart",
                    broadcastQueueClass,
                    broadcastRecordClass,
                    ResolveInfo::class.java
                ) {
                    try {
                        val broadcastRecord = getArg(1) ?: return@chainMethod proceed()
                        val callerPackage = getObjectField(broadcastRecord, "callerPackage") as? String
                            ?: return@chainMethod proceed()
                        val intent = getObjectField(broadcastRecord, "intent") as? Intent
                            ?: return@chainMethod proceed()
                        if (callerPackage == gmsPackageName && intent.action == actionRemoteIntent) {
                            return@chainMethod true
                        }
                    } catch (e: Exception) {
                        XposedLog.e(TAG, packageName, "Failed to modify checkApplicationAutoStart", e)
                    }
                    proceed()
                }
                clazz.getDeclaredMethod(
                    "checkApplicationAutoStart",
                    broadcastQueueClass,
                    broadcastRecordClass,
                    ResolveInfo::class.java
                ).deoptimizeMethod()
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in checkApplicationAutoStart", e)
            }
        }

        processPolicyClass?.let { clazz ->
            try {
                clazz.chainMethod(
                    "getWhiteList",
                    Int::class.java
                ) {
                    val result = proceed()
                    try {
                        val flags = getArg(0) as? Int ?: return@chainMethod result
                        if ((flags and 1) == 0) return@chainMethod result

                        val list = when (result) {
                            is MutableList<*> -> result as MutableList<Any?>
                            is List<*> -> result.toMutableList()
                            else -> return@chainMethod result
                        }

                        if (!list.contains(gmsPackageName)) list.add(gmsPackageName)
                        if (!list.contains(gmsPersistentProcessName)) list.add(gmsPersistentProcessName)
                        list
                    } catch (e: Exception) {
                        XposedLog.e(TAG, packageName, "Failed to modify getWhiteList", e)
                        result
                    }
                }
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in getWhiteList", e)
            }
        }

        awareResourceControlClass?.let { clazz ->
            try {
                chainAllConstructors(clazz) { chain ->
                    val result = chain.proceed()
                    try {
                        val thisObject = chain.thisObject ?: return@chainAllConstructors result
                        val noNetworkBlackUids = getObjectField(thisObject, "mNoNetworkBlackUids")
                            as? MutableCollection<Any?>
                            ?: return@chainAllConstructors result
                        noNetworkBlackUids.remove(gmsPackageName)
                    } catch (e: Exception) {
                        XposedLog.e(TAG, packageName, "Failed to modify mNoNetworkBlackUids", e)
                    }
                    result
                }
                clazz.declaredConstructors.forEach { constructor ->
                    constructor.deoptimizeConstructor()
                }
            } catch (e: Exception) {
                XposedLog.e(TAG, packageName, "Hook Failed in AwareResourceControl constructor", e)
            }
        }
    }

}
