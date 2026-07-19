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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.PowerExemptionManager
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.interceptHookMethod
import io.github.lingqiqi5211.ezhooktool.xposed.java.Deoptimizers

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
    private const val GMS_PACKAGE_NAME = "com.google.android.gms"
    private const val GMS_PERSISTENT_PROCESS_NAME = "com.google.android.gms.persistent"
    private const val ACTION_REMOTE_INTENT = "com.google.android.c2dm.intent.RECEIVE"

    private var powerExemptionManager: PowerExemptionManager? = null

    private fun getPowerExemptionManager(context: Context): PowerExemptionManager {
        if (powerExemptionManager == null) {
            powerExemptionManager = PowerExemptionManager(context)
        }
        return powerExemptionManager!!
    }

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
        val activityManagerServiceClass =
            findClassIfExists("com.android.server.am.ActivityManagerService")

        hookGreezeManagerService(greezeManagerServiceClass)
        hookDomesticPolicyManager(domesticPolicyManagerClass)
        hookListAppsManager(listAppsManagerClass)
        hookBroadcastQueueModernStubImpl(
            broadcastQueueModernStubImplClass, broadcastQueueClass, broadcastRecordClass
        )
        hookProcessPolicy(processPolicyClass)
        hookAwareResourceControl(awareResourceControlClass)
        hookActivityManagerService(activityManagerServiceClass)
    }

    private fun hookGreezeManagerService(clazz: Class<*>?) {
        clazz ?: return
        try {
            val getPackageNameFromUidMethod = clazz.getDeclaredMethod(
                "getPackageNameFromUid", Int::class.java
            )
            getPackageNameFromUidMethod.isAccessible = true
            clazz.interceptHookMethod(
                "isAllowBroadcast",
                Int::class.java,
                String::class.java,
                Int::class.java,
                String::class.java,
                String::class.java
            ) { chain ->
                var calleePkgName = chain.getArg(3) as? String
                try {
                    val calleeUid = chain.getArg(2) as? Int
                    if (calleeUid != null) {
                        val resolved = com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeOriginalMethod(
                            getPackageNameFromUidMethod, chain.thisObject, calleeUid
                        ) as? String
                        if (resolved != null) {
                            calleePkgName = resolved
                        }
                    }
                } catch (e: Exception) {
                    XposedLog.e(TAG, packageName, "Failed to get callee package name", e)
                }
                val action = chain.getArg(4) as? String
                if (action != null) {
                    val callerPkgName = chain.getArg(1) as? String
                    if ((callerPkgName == GMS_PACKAGE_NAME && action == ACTION_REMOTE_INTENT) ||
                        ((calleePkgName == GMS_PACKAGE_NAME || calleePkgName == GMS_PERSISTENT_PROCESS_NAME) &&
                            action in cnDeferBroadcast)
                    ) {
                        return@interceptHookMethod true
                    }
                }
                chain.proceed()
            }
            clazz.getDeclaredMethod(
                "isAllowBroadcast",
                Int::class.java,
                String::class.java,
                Int::class.java,
                String::class.java,
                String::class.java
            ).let(Deoptimizers::deoptimize)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in isAllowBroadcast: ", e)
        }

        try {
            clazz.interceptHookMethod(
                "deferBroadcastForMiui",
                String::class.java
            ) { chain ->
                val action = chain.getArg(0) as? String ?: return@interceptHookMethod chain.proceed()
                if (action in cnDeferBroadcast) {
                    return@interceptHookMethod false
                }
                chain.proceed()
            }
            clazz.getDeclaredMethod(
                "deferBroadcastForMiui",
                String::class.java
            ).let(Deoptimizers::deoptimize)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in deferBroadcastForMiui: ", e)
        }

        try {
            clazz.interceptHookMethod(
                "triggerGMSLimitAction",
                Boolean::class.java
            ) { chain ->
                val methodArgs = chain.args.toMutableList()
                if (methodArgs.isNotEmpty()) {
                    methodArgs[0] = false
                }
                chain.proceed(methodArgs.toTypedArray())
            }
            clazz.getDeclaredMethod(
                "triggerGMSLimitAction",
                Boolean::class.java
            ).let(Deoptimizers::deoptimize)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in triggerGMSLimitAction: ", e)
        }
    }

    private fun hookDomesticPolicyManager(clazz: Class<*>?) {
        clazz ?: return
        try {
            clazz.interceptHookMethod(
                "deferBroadcast",
                String::class.java
            ) {
                false
            }
            clazz.getDeclaredMethod(
                "deferBroadcast",
                String::class.java
            ).let(Deoptimizers::deoptimize)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in deferBroadcast: ", e)
        }
    }

    private fun hookListAppsManager(clazz: Class<*>?) {
        clazz ?: return
        try {
            chainAllConstructors(clazz) { chain ->
                val result = chain.proceed()
                try {
                    val thisObject = chain.thisObject ?: return@chainAllConstructors result
                    val blackList = getObjectField(thisObject, "mSystemBlackList")
                        as? MutableCollection<Any?>
                        ?: return@chainAllConstructors result
                    blackList.remove(GMS_PACKAGE_NAME)
                } catch (e: Exception) {
                    XposedLog.e(TAG, packageName, "Failed to modify mSystemBlackList", e)
                }
                result
            }
            clazz.declaredConstructors.forEach { constructor ->
                Deoptimizers.deoptimize(constructor)
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in ListAppsManager constructor", e)
        }
    }

    private fun hookBroadcastQueueModernStubImpl(
        clazz: Class<*>?,
        broadcastQueueClass: Class<*>?,
        broadcastRecordClass: Class<*>?
    ) {
        if (clazz == null || broadcastQueueClass == null || broadcastRecordClass == null) return
        try {
            clazz.interceptHookMethod(
                "checkApplicationAutoStart",
                broadcastQueueClass,
                broadcastRecordClass,
                ResolveInfo::class.java
            ) { chain ->
                try {
                    val broadcastRecord = chain.getArg(1) ?: return@interceptHookMethod chain.proceed()
                    val callerPackage = getObjectField(broadcastRecord, "callerPackage") as? String
                        ?: return@interceptHookMethod chain.proceed()
                    val intent = getObjectField(broadcastRecord, "intent") as? Intent
                        ?: return@interceptHookMethod chain.proceed()
                    if (callerPackage == GMS_PACKAGE_NAME && intent.action == ACTION_REMOTE_INTENT) {
                        return@interceptHookMethod true
                    }
                } catch (e: Exception) {
                    XposedLog.e(TAG, packageName, "Failed to modify checkApplicationAutoStart", e)
                }
                chain.proceed()
            }
            clazz.getDeclaredMethod(
                "checkApplicationAutoStart",
                broadcastQueueClass,
                broadcastRecordClass,
                ResolveInfo::class.java
            ).let(Deoptimizers::deoptimize)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in checkApplicationAutoStart", e)
        }
    }

    private fun hookProcessPolicy(clazz: Class<*>?) {
        clazz ?: return
        try {
            clazz.interceptHookMethod(
                "getWhiteList",
                Int::class.java
            ) { chain ->
                val result = chain.proceed()
                try {
                    val flags = chain.getArg(0) as? Int ?: return@interceptHookMethod result
                    if ((flags and 1) == 0) return@interceptHookMethod result

                    val list = when (result) {
                        is MutableList<*> -> result as MutableList<Any?>
                        is List<*> -> result.toMutableList()
                        else -> return@interceptHookMethod result
                    }

                    if (!list.contains(GMS_PACKAGE_NAME)) list.add(GMS_PACKAGE_NAME)
                    if (!list.contains(GMS_PERSISTENT_PROCESS_NAME)) list.add(
                        GMS_PERSISTENT_PROCESS_NAME
                    )
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

    private fun hookAwareResourceControl(clazz: Class<*>?) {
        clazz ?: return
        try {
            chainAllConstructors(clazz) { chain ->
                val result = chain.proceed()
                try {
                    val thisObject = chain.thisObject ?: return@chainAllConstructors result
                    val noNetworkBlackUids = getObjectField(thisObject, "mNoNetworkBlackUids")
                        as? MutableCollection<Any?>
                        ?: return@chainAllConstructors result
                    noNetworkBlackUids.remove(GMS_PACKAGE_NAME)
                } catch (e: Exception) {
                    XposedLog.e(TAG, packageName, "Failed to modify mNoNetworkBlackUids", e)
                }
                result
            }
            clazz.declaredConstructors.forEach { constructor ->
                Deoptimizers.deoptimize(constructor)
            }
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in AwareResourceControl constructor", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun hookActivityManagerService(clazz: Class<*>?) {
        clazz ?: return
        try {
            val mContextField = clazz.getDeclaredField("mContext")
            mContextField.isAccessible = true

            val iApplicationThreadClass =
                findClassIfExists("android.app.IApplicationThread") ?: return
            val iIntentReceiverClass =
                findClassIfExists("android.content.IIntentReceiver") ?: return
            val processRecordClass =
                findClassIfExists("com.android.server.am.ProcessRecord") ?: return
            val infoField = processRecordClass.getDeclaredField("info")
            infoField.isAccessible = true

            val getRecordMethod =
                clazz.getDeclaredMethod("getRecordForAppLOSP", iApplicationThreadClass)

            // broadcastIntentWithFeature (TIRAMISU+ / API 33+, 16 params)
            val intentArgIndex = 2
            val broadcastMethod = clazz.getDeclaredMethod(
                "broadcastIntentWithFeature",
                iApplicationThreadClass, String::class.java,
                Intent::class.java, String::class.java, iIntentReceiverClass,
                Int::class.java, String::class.java, Bundle::class.java,
                Array<String>::class.java, Array<String>::class.java,
                Array<String>::class.java, Int::class.java, Bundle::class.java,
                Boolean::class.java, Boolean::class.java, Int::class.java
            )

            chain(broadcastMethod) { chain ->
                val intent = chain.getArg(intentArgIndex) as? Intent
                if (intent != null && ACTION_REMOTE_INTENT == intent.action) {
                    val app = com.sevtinge.hyperceiler.libhook.base.BaseHook.invokeOriginalMethod(
                        getRecordMethod, chain.thisObject, chain.getArg(0)
                    )
                    if (app != null) {
                        val info = infoField.get(app) as? ApplicationInfo
                        if (info != null && GMS_PACKAGE_NAME == info.packageName) {
                            val targetPackage = intent.`package`
                            val mContext = mContextField.get(chain.thisObject) as? Context
                            if (targetPackage != null && mContext != null) {
                                getPowerExemptionManager(mContext).addToTemporaryAllowList(
                                    targetPackage,
                                    102, // PowerExemptionManager.REASON_PUSH_MESSAGING_OVER_QUOTA
                                    "GOOGLE_C2DM",
                                    2000
                                )
                            }
                            if ((intent.flags and Intent.FLAG_INCLUDE_STOPPED_PACKAGES) == 0) {
                                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            }
                        }
                    }
                }
                chain.proceed()
            }
            Deoptimizers.deoptimize(broadcastMethod)
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in ActivityManagerService", e)
        }
    }
}
