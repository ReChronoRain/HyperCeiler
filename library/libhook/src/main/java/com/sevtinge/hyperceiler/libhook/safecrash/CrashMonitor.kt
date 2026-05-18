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
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.callback.ICrashHandler
import com.sevtinge.hyperceiler.libhook.utils.hookapi.PackageWatchdog
import io.github.libxposed.api.XposedModuleInterface
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createInterceptHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getAdditionalInstanceFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.interceptHookMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.removeAdditionalInstanceField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setAdditionalInstanceField
import java.lang.reflect.Constructor

class CrashMonitor(lpparam: XposedModuleInterface.SystemServerStartingParam) {
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
        val bgControllerClassName = "com.android.server.wm.BackgroundActivityStartController"
        val balStateClassName = $$"$$bgControllerClassName$BalState"
        val balVerdictClassName = $$"$$bgControllerClassName$BalVerdict"

        val bgControllerClass = com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists(bgControllerClassName, classLoader)
        val balStateClass = com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists(balStateClassName, classLoader)
        val allowVerdictFactory = resolveAllowByDefaultVerdictFactory(classLoader, balVerdictClassName)

        if (bgControllerClass == null) {
            XposedLog.w(TAG, "BackgroundActivityStartController not found, skip BAS hooks")
        } else {
            val chainHookInstalled = if (balStateClass != null && allowVerdictFactory != null) {
                hookBackgroundActivityStartChain(bgControllerClass, balStateClass, allowVerdictFactory)
            } else {
                false
            }

            if (!chainHookInstalled) {
                hookBackgroundActivityStartFallback(bgControllerClass, allowVerdictFactory)
            }
        }

        hookActivityStarterImpl(classLoader)
    }

    private fun hookBackgroundActivityStartChain(
        bgControllerClass: Class<*>,
        balStateClass: Class<*>,
        allowVerdictFactory: () -> Any
    ): Boolean {
        return runCatching {
            bgControllerClass.interceptHookMethod("abortLaunch", balStateClass) { chain ->
                val state = chain.getArg(0)
                if (shouldAllowBackgroundActivityStart(state)) {
                    return@interceptHookMethod allowVerdictFactory()
                }
                chain.proceed()
            }
            XposedLog.i(TAG, "Installed BAL chain hook: abortLaunch")
            true
        }.onFailure { throwable ->
            XposedLog.w(TAG, "Failed to install BAL chain hook: abortLaunch", throwable)
        }.getOrDefault(false)
    }

    private fun hookBackgroundActivityStartFallback(
        bgControllerClass: Class<*>,
        allowVerdictFactory: (() -> Any)?
    ) {
        if (allowVerdictFactory == null) {
            XposedLog.w(TAG, "BalVerdict factory unavailable, skip BAS fallback hook")
            return
        }

        runCatching {
            bgControllerClass.findMethod { name("checkBackgroundActivityStart"); paramCount(11) }
                .createInterceptHook { chain ->
                    val pkg = chain.getArg(2) as? String
                    if (isModulePackage(pkg)) {
                        return@createInterceptHook allowVerdictFactory()
                    }
                    chain.proceed()
                }
        }.onSuccess {
            XposedLog.i(TAG, "Installed BAS fallback hook on checkBackgroundActivityStart")
        }.onFailure { throwable ->
            XposedLog.e(TAG, "Failed to hook checkBackgroundActivityStart", throwable)
        }
    }

    private fun hookActivityStarterImpl(classLoader: ClassLoader) {
        val stubClassName = "com.android.server.wm.ActivityStarterStub"
        val stubClass = com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists(stubClassName, classLoader)
        if (stubClass == null) {
            XposedLog.w(TAG, "ActivityStarterStub not found, skip MIUI allow hook")
            return
        }

        runCatching {
            stubClass.interceptHookMethod(
                "isAllowedStartActivity",
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                String::class.java
            ) { chain ->
                val pkg = chain.getArg(2) as? String
                if (isModulePackage(pkg)) {
                    return@interceptHookMethod true
                }
                chain.proceed()
            }
            XposedLog.i(TAG, "Installed ActivityStarter allow hook: $stubClassName#isAllowedStartActivity(int,int,String)")
        }.onFailure { throwable ->
            XposedLog.w(TAG, "Failed to install ActivityStarter allow hook", throwable)
        }
    }

    private fun resolveAllowByDefaultVerdictFactory(
        classLoader: ClassLoader,
        balVerdictClassName: String
    ): (() -> Any)? {
        val balVerdictClass = com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists(balVerdictClassName, classLoader)
            ?: run {
                XposedLog.w(TAG, "BalVerdict class not found: $balVerdictClassName")
                return null
            }

        val constructor = findAllowByDefaultConstructor(balVerdictClass)
        if (constructor != null) {
            return {
                when (constructor.parameterTypes.size) {
                    3 -> constructor.newInstance(1, false, "Default")
                    else -> constructor.newInstance(1, "Default")
                }
            }
        }

        val sharedVerdict = listOf("ALLOW_BY_DEFAULT", "BAL_ALLOW_DEFAULT")
            .firstNotNullOfOrNull { fieldName ->
                runCatching {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.getStaticObjectField(balVerdictClass, fieldName)
                }.getOrNull()
            }

        if (sharedVerdict != null) {
            XposedLog.w(TAG, "Falling back to shared BalVerdict instance; verdict flags may be reused")
            return { sharedVerdict }
        }

        XposedLog.w(TAG, "Unable to resolve allow-by-default BalVerdict factory")
        return null
    }

    private fun findAllowByDefaultConstructor(balVerdictClass: Class<*>): Constructor<*>? {
        return runCatching {
            balVerdictClass.declaredConstructors.firstOrNull { constructor ->
                val parameterTypes = constructor.parameterTypes
                when (parameterTypes.size) {
                    2 -> {
                        parameterTypes[0] == Int::class.javaPrimitiveType &&
                            parameterTypes[1] == String::class.java
                    }

                    3 -> {
                        parameterTypes[0] == Int::class.javaPrimitiveType &&
                            parameterTypes[1] == Boolean::class.javaPrimitiveType &&
                            parameterTypes[2] == String::class.java
                    }

                    else -> false
                }
            }?.apply {
                isAccessible = true
            }
        }.getOrNull()
    }

    private fun shouldAllowBackgroundActivityStart(state: Any?): Boolean {
        if (state == null) return false

        val callingPackage = runCatching {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(state, "mCallingPackage") as? String
        }.getOrNull()
        if (isModulePackage(callingPackage)) {
            return true
        }

        val realCallingPackage = runCatching {
            com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(state, "mRealCallingPackage") as? String
        }.getOrNull()
        return isModulePackage(realCallingPackage)
    }

    private fun isModulePackage(pkg: String?): Boolean {
        return pkg == ProjectApi.mAppModulePkg
    }

    /**
     * Hook AppErrors.handleAppCrashInActivityController
     */
    private fun hookAppErrors(classLoader: ClassLoader) {
        val appErrorsClass =
            com.sevtinge.hyperceiler.libhook.base.BaseHook.findClassIfExists("com.android.server.am.AppErrors", classLoader)
                ?: throw ClassNotFoundException("com.android.server.am.AppErrors not found")

        appErrorsClass.findMethod { name("handleAppCrashInActivityController"); returnType(Boolean::class.java) }
            .createHook {
                after { param ->
                    val mContext = com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.thisObject, "mContext") as Context
                    val proc = param.args[0] ?: run {
                        XposedLog.w(TAG, "Crash callback received null process record, skip crash handling")
                        return@after
                    }
                    val crashInfo = param.args[1] as? ApplicationErrorReport.CrashInfo
                    //  val shortMsg = param.args[2] as? String
                    val longMsg = param.args[3] as? String
                    val stackTrace = param.args[4] as? String
                    val timeMillis = param.args[5] as Long
                    // val callingPid = param.args[6] as Int
                    // val callingUid = param.args[7] as Int

                    // 获取包名
                    val info = com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(proc, "info") as? ApplicationInfo
                    val pkgName = info?.packageName ?: return@after

                    if (!CrashScope.isScopeApp(pkgName)) return@after
                    // 已经受不了你们反馈无关 Crash 了
                    XposedLog.e(TAG, "Crash detected: $pkgName, log: $stackTrace")
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
            com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("com.android.server.PackageWatchdogImpl", classLoader)

        // 拦截 setCrashApplicationLevel
        watchdogClass.findMethod { name("setCrashApplicationLevel"); parameterTypes(Int::class.java, VersionedPackage::class.java, Context::class.java) }
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
        watchdogClass.findAllMethods { parameterTypes(Int::class.java, VersionedPackage::class.java, Context::class.java); filter { name == "doRescuePartyPlusStepNew" || name == "doRescuePartyPlusStep" } }
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
