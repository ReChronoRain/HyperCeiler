package com.sevtinge.hyperceiler.hook.safe

import android.content.Context
import android.content.pm.VersionedPackage
import android.os.SystemProperties
import android.provider.Settings
import com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_HOME
import com.sevtinge.hyperceiler.hook.module.HostConstant.HOST_SYSTEM_UI
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.hook.utils.hidden.PackageWatchdog
import com.sevtinge.hyperceiler.hook.utils.removeAdditionalInstanceField
import com.sevtinge.hyperceiler.hook.utils.setAdditionalInstanceField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object RescuePartyPlus : BaseHook() {
    private lateinit var handler: CrashHandler

    override fun init() {
        if (!::handler.isInitialized) {
            return
        }

        PackageWatchdog.setClassLoader(lpparam.classLoader)

        val packageWatchdogImpl = findClass("com.android.server.PackageWatchdogImpl")
        packageWatchdogImpl.methodFinder()
            .filterByName("setCrashApplicationLevel")
            .filterByParamTypes(Int::class.java, VersionedPackage::class.java, Context::class.java)
            .first().createBeforeHook { param ->
                val versionedPackage = param.args[1] as VersionedPackage?
                if (checkDisableRescuePartyPlus() || versionedPackage == null) {
                    param.result = false
                    return@createBeforeHook
                }

                val mitigationCount = param.args[0] as Int
                val context = param.args[2] as Context

                logE(
                    TAG, lpparam.packageName,
                    "setCrashApplicationLevel($versionedPackage, $mitigationCount)"
                )

                val packageName = versionedPackage.packageName
                when (packageName) {
                    HOST_SYSTEM_UI -> {
                        if (handler.onHandleCrash(context, packageName, mitigationCount)) {
                            putGlobalSettings(context, "sys.rescueparty.systemui.level", 0)
                            putGlobalSettings(context, "sys.anr.rescue.systemui.level", 0)
                            onAfterSetAppCrashLevel(context, packageName, param.thisObject)
                            param.result = true
                        }
                    }

                    HOST_HOME -> {
                        if (handler.onHandleCrash(context, packageName, mitigationCount)) {
                            putGlobalSettings(context, "sys.rescueparty.home.level", 0)
                            onAfterSetAppCrashLevel(context, packageName, param.thisObject)
                            param.result = true
                        }
                    }
                }
            }

        packageWatchdogImpl.methodFinder()
            .filter {
                name == "doRescuePartyPlusStepNew" || name == "doRescuePartyPlusStep"
            }
            .filterByParamTypes(Int::class.java, VersionedPackage::class.java, Context::class.java)
            .first().createBeforeHook { param ->
                val watchdog = param.thisObject
                val flag = watchdog.getAdditionalInstanceFieldAs<String?>("flag")
                if (flag == null) {
                    return@createBeforeHook
                }

                watchdog.removeAdditionalInstanceField("flag")
                val versionedPackage = param.args[1] as VersionedPackage?
                if (versionedPackage == null) {
                    param.result = false
                    return@createBeforeHook
                }

                val mitigationCount = param.args[0] as Int
                val packageName = versionedPackage.packageName
                if (packageName != flag) {
                    return@createBeforeHook
                }

                if (mitigationCount > 1) {
                    watchdog.callMethod(
                        "removeMessage",
                        if (mitigationCount <= 7) {
                            mitigationCount - 1
                        } else {
                            7
                        },
                        packageName
                    )
                }

                param.result = true
            }
    }

    fun setHandler(handler: CrashHandler) {
        this.handler = handler
    }

    private fun putGlobalSettings(context: Context, key: String, value: Int) {
        Settings.Global.putInt(context.contentResolver, key, value)
    }

    private fun onAfterSetAppCrashLevel(context: Context, packageName: String, watchdog: Any) {
        SystemProperties.set("sys.set_app_crash_level.flag", "true")
        PackageWatchdog.clearRecord(context, packageName)
        watchdog.setAdditionalInstanceField("flag", packageName)
    }

    private fun checkDisableRescuePartyPlus(): Boolean {
        if (SystemProperties.getBoolean("persist.sys.rescuepartyplus.disable", false)) {
            return true
        }

        if (!SystemProperties.getBoolean("persist.sys.rescuepartyplus.enable", false)) {
            return false
        }

        return false
    }

    interface CrashHandler {
        fun onHandleCrash(context: Context, pkgName: String, mitigationCount: Int): Boolean
    }
}
