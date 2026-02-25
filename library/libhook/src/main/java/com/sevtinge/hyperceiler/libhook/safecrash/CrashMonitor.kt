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
package com.sevtinge.hyperceiler.libhook.safecrash

import android.app.ApplicationErrorReport
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.VersionedPackage
import android.os.SystemProperties
import android.provider.Settings
import com.sevtinge.hyperceiler.libhook.callback.ICrashHandler
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.utils.hookapi.PackageWatchdog
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.removeAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHooks
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import io.github.libxposed.api.XposedModuleInterface

class CrashMonitor(lpparam: XposedModuleInterface.SystemServerLoadedParam) {
    private val crashHandler: ICrashHandler = SafeModeHandler

    companion object {
        private const val TAG = "CrashMonitor"
        private const val RECORD_KEY = "hyperceiler_crash_record_data"
        private const val CRASH_TIME_WINDOW = 60000L
        private const val TRIGGER_INTERVAL = 10240L
        private const val TRIGGER_COUNT = 2
    }

    init {
        val classLoader = lpparam.classLoader

        hookBackgroundActivityStart(classLoader)

        hookAppErrors(classLoader)

        hookRescueParty(classLoader)
    }

    /**
     * 允许 HyperCeiler 在后台启动界面
     */
    private fun hookBackgroundActivityStart(classLoader: ClassLoader) {
        val bgControllerClass = "com.android.server.wm.BackgroundActivityStartController"
        val balVerdictClass = $$"$$bgControllerClass$BalVerdict"

        /*val paramTypes = if (DeviceHelper.System.isMoreAndroidVersion(36)) {
            arrayOf(
                Int::class.java, Int::class.java, String::class.java, Int::class.java,
                "com.android.server.wm.WindowProcessController",
                "com.android.server.am.PendingIntentRecord",
                Boolean::class.java,
                "com.android.server.wm.ActivityRecord",
                Intent::class.java,
                ActivityOptions::class.java
            )
        } else {
            arrayOf(
                Int::class.java, Int::class.java, String::class.java, Int::class.java, Int::class.java,
                "com.android.server.wm.WindowProcessController",
                "com.android.server.am.PendingIntentRecord",
                "android.app.BackgroundStartPrivileges",
                "com.android.server.wm.ActivityRecord",
                Intent::class.java,
                ActivityOptions::class.java
            )
        }*/

        EzxHelpUtils.findClassIfExists(bgControllerClass, classLoader)?.let { clazz ->
            try {
                clazz.methodFinder()
                    .filterByName("checkBackgroundActivityStart")
                    .first()
                    .createBeforeHook { param ->
                        val pkg = param.args[2] as? String
                        if (pkg == ProjectApi.mAppModulePkg) {
                            val balAllowDefault = EzxHelpUtils.getStaticObjectField(
                                EzxHelpUtils.findClass(balVerdictClass, classLoader),
                                "BAL_ALLOW_DEFAULT"
                            )
                            if (balAllowDefault != null) {
                                param.result = balAllowDefault
                            } else {
                                XposedLog.w(TAG, "BAL_ALLOW_DEFAULT is null, skipping hook")
                            }
                        }
                    }
            } catch (e: Exception) {
                XposedLog.e(TAG, "Failed to hook checkBackgroundActivityStart", e)
            }
        }

        EzxHelpUtils.hookAllMethods("com.android.server.wm.ActivityStarterImpl", classLoader, "isAllowedStartActivity",
            object : IMethodHook {
                override fun before(param: BeforeHookParam) {
                    val pkg = param.args.firstOrNull { it is String } as? String
                    if (pkg == ProjectApi.mAppModulePkg) {
                        param.result = true
                    }
                }
            })

        EzxHelpUtils.hookAllMethods("com.android.server.wm.ActivityStarterImpl", classLoader, "isAllowedStartActivity", object : IMethodHook {
            override fun before(param: BeforeHookParam) {
                val pkg = param.args.firstOrNull { it is String } as? String
                if (pkg == ProjectApi.mAppModulePkg) {
                    param.result = true
                }
            }
        })
    }

    /**
     * Hook AppErrors.handleAppCrashInActivityController
     */
    private fun hookAppErrors(classLoader: ClassLoader) {
        val appErrorsClass =
            EzxHelpUtils.findClassIfExists("com.android.server.am.AppErrors", classLoader)
                ?: throw ClassNotFoundException("com.android.server.am.AppErrors not found")

        appErrorsClass.methodFinder()
            .filterByName("handleAppCrashInActivityController")
            .filterByReturnType(Boolean::class.java)
            .first()
            .createHook {
                after { param ->
                    val mContext = EzxHelpUtils.getObjectField(param.thisObject, "mContext") as Context
                    val proc = param.args[0] // ProcessRecord
                    val crashInfo = param.args[1] as? ApplicationErrorReport.CrashInfo
                    //  val shortMsg = param.args[2] as? String
                    val longMsg = param.args[3] as? String
                    val stackTrace = param.args[4] as? String
                    val timeMillis = param.args[5] as Long
                    // val callingPid = param.args[6] as Int
                    // val callingUid = param.args[7] as Int

                    // 获取包名
                    val info = EzxHelpUtils.getObjectField(proc!!, "info") as? ApplicationInfo
                    val pkgName = info?.packageName ?: return@after

                    XposedLog.e(TAG, "Crash detected: $pkgName, log: $longMsg")

                    handleCrashLogic(mContext, pkgName, timeMillis, crashInfo, longMsg, stackTrace)
                }
            }
    }

    /**
     * Hook PackageWatchdog
     */
    private fun hookRescueParty(classLoader: ClassLoader) {
        PackageWatchdog.setClassLoader(classLoader)
        val watchdogClass =
            EzxHelpUtils.findClass("com.android.server.PackageWatchdogImpl", classLoader)

        // 拦截 setCrashApplicationLevel
        watchdogClass.methodFinder()
            .filterByName("setCrashApplicationLevel")
            .filterByParamTypes(Int::class.java, VersionedPackage::class.java, Context::class.java)
            .first()
            .createBeforeHook { param ->
                val mitigationCount = param.args[0] as Int
                val versionedPackage = param.args[1] as? VersionedPackage ?: return@createBeforeHook
                val context = param.args[2] as Context
                val pkgName = versionedPackage.packageName

                if (shouldDisableRescuePartyPlus()) {
                    param.result = false
                    return@createBeforeHook
                }

                // 只处理我们关注的 App
                if (!CrashScope.isScopeApp(pkgName)) return@createBeforeHook

                XposedLog.e(TAG, "RescueParty triggered for $pkgName, level: $mitigationCount")

                val handled = crashHandler.onCrashDetected(context, pkgName, null, "RescueParty Triggered", "System detected frequent crashes")

                if (handled) {
                    // 如果我们处理了，就告诉系统不要继续 RescueParty
                    resetSystemRescueLevel(context, pkgName)

                    // 标记该次处理，防止后续步骤继续执行
                    onAfterSetAppCrashLevel(context, pkgName, param.thisObject)
                    param.result = true
                }
            }

        // 拦截具体的 RescueParty 步骤
        watchdogClass.methodFinder()
            .filter { name == "doRescuePartyPlusStepNew" || name == "doRescuePartyPlusStep" }
            .filterByParamTypes(Int::class.java, VersionedPackage::class.java, Context::class.java)
            .toList()
            .createBeforeHooks { param ->
                val watchdog = param.thisObject
                val flagPkg = watchdog.getAdditionalInstanceFieldAs<String?>("flag_pkg")
                    ?: return@createBeforeHooks

                watchdog.removeAdditionalInstanceField("flag_pkg")
                val versionedPackage =
                    param.args[1] as? VersionedPackage ?: return@createBeforeHooks

                if (versionedPackage.packageName == flagPkg) {
                    val mitigationCount = param.args[0] as Int
                    val msgId = if (mitigationCount <= 7) mitigationCount - 1 else 7
                    watchdog.callMethod("removeMessage", msgId, flagPkg)

                    param.result = true
                }
            }
    }

    /**
     * 处理崩溃记录逻辑
     */
    private fun handleCrashLogic(
        context: Context,
        pkgName: String,
        timeMillis: Long,
        crashInfo: ApplicationErrorReport.CrashInfo?,
        longMsg: String?,
        stackTrace: String?
    ) {
        if (!CrashScope.isScopeApp(pkgName)) return

        // 读取历史记录
        val records = getCrashRecords(context)
        val iterator = records.iterator()

        var shouldReport = false
        var updated = false

        // 清理超时记录
        while (iterator.hasNext()) {
            val record = iterator.next()
            if (timeMillis - record.time > CRASH_TIME_WINDOW) {
                iterator.remove()
                updated = true
                continue
            }

            if (record.pkg == pkgName) {
                if (timeMillis - record.time < TRIGGER_INTERVAL) {
                    if (record.count >= TRIGGER_COUNT) {
                        shouldReport = true
                    } else {
                        val newRecord = record.copy(time = timeMillis, count = record.count + 1)
                        iterator.remove()
                        records.add(newRecord)
                        updated = true
                    }
                } else {
                    iterator.remove()
                    updated = true
                }
                break
            }
        }

        // 如果没有找到记录，且没触发报告，则添加新记录
        if (!shouldReport && !records.any { it.pkg == pkgName }) {
            records.add(DataCrashRecord(pkgName, timeMillis, 1))
            updated = true
        }

        // 保存记录
        if (updated) {
            setCrashRecords(context, records)
        }

        // 触发扩展处理
        if (shouldReport) {
            records.removeIf { it.pkg == pkgName }
            setCrashRecords(context, records)

            crashHandler.onCrashDetected(context, pkgName, crashInfo, longMsg, stackTrace)
        }
    }

    // --- 辅助方法 ---

    private fun getCrashRecords(context: Context): MutableList<DataCrashRecord> {
        val json = Settings.System.getString(context.contentResolver, RECORD_KEY)
        return DataCrashRecord.parseList(json)
    }

    private fun setCrashRecords(context: Context, list: List<DataCrashRecord>) {
        val json = DataCrashRecord.listToJsonString(list)
        Settings.System.putString(context.contentResolver, RECORD_KEY, json)
    }

    private fun shouldDisableRescuePartyPlus(): Boolean {
        return SystemProperties.getBoolean("persist.sys.rescuepartyplus.disable", false) ||
            !SystemProperties.getBoolean("persist.sys.rescuepartyplus.enable", false)
    }

    private fun resetSystemRescueLevel(context: Context, pkgName: String) {
        // 重置系统设置中的救援等级，防止系统重启
        if (pkgName == "com.android.systemui") {
            Settings.Global.putInt(context.contentResolver, "sys.rescueparty.systemui.level", 0)
            Settings.Global.putInt(context.contentResolver, "sys.anr.rescue.systemui.level", 0)
        } else if (pkgName == "com.miui.home") {
            Settings.Global.putInt(context.contentResolver, "sys.rescueparty.home.level", 0)
        }
    }

    private fun onAfterSetAppCrashLevel(context: Context, pkgName: String, watchdog: Any) {
        SystemProperties.set("sys.set_app_crash_level.flag", "true")
        PackageWatchdog.clearRecord(context, pkgName)
        watchdog.setAdditionalInstanceField("flag_pkg", pkgName)
    }
}
