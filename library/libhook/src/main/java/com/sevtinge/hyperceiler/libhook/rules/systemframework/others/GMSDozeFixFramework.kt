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
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
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
            clazz.chainMethod(
                "isAllowBroadcast",
                Int::class.java,
                String::class.java,
                Int::class.java,
                String::class.java,
                String::class.java
            ) {
                var calleePkgName = getArg(3) as? String
                try {
                    val calleeUid = getArg(2) as? Int
                    if (calleeUid != null) {
                        val resolved = EzxHelpUtils.invokeOriginalMethod(
                            getPackageNameFromUidMethod, thisObject, calleeUid
                        ) as? String
                        if (resolved != null) {
                            calleePkgName = resolved
                        }
                    }
                } catch (e: Exception) {
                    XposedLog.e(TAG, packageName, "Failed to get callee package name", e)
                }
                val action = getArg(4) as? String
                if (action != null) {
                    val callerPkgName = getArg(1) as? String
                    if ((callerPkgName == GMS_PACKAGE_NAME && action == ACTION_REMOTE_INTENT) ||
                        ((calleePkgName == GMS_PACKAGE_NAME || calleePkgName == GMS_PERSISTENT_PROCESS_NAME) &&
                            action in cnDeferBroadcast)
                    ) {
                        return@chainMethod true
                    }
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

    private fun hookDomesticPolicyManager(clazz: Class<*>?) {
        clazz ?: return
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
                constructor.deoptimizeConstructor()
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
                    if (callerPackage == GMS_PACKAGE_NAME && intent.action == ACTION_REMOTE_INTENT) {
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

    private fun hookProcessPolicy(clazz: Class<*>?) {
        clazz ?: return
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
                constructor.deoptimizeConstructor()
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
                    val app = EzxHelpUtils.invokeOriginalMethod(
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
            broadcastMethod.deoptimizeMethod()
        } catch (e: Exception) {
            XposedLog.e(TAG, packageName, "Hook Failed in ActivityManagerService", e)
        }
    }
}
