package com.sevtinge.cemiuiler.utils.log

import com.sevtinge.cemiuiler.BuildConfig
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object XposedLogUtils {
    private var isDebugVersion = !BuildConfig.BUILD_TYPE.contains("release")
    private val detailLog = BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log")

    // Xposed debug 日志输出
    fun logI(tag: String, msg: String) {
        if (!isDebugVersion) return
        if (detailLog) return
        XposedBridge.log("[I/Cemiuiler]: [$tag] $msg")
    }

    fun logW(tag: String, log: Throwable) {
        if (detailLog) return
        XposedBridge.log("[W/Cemiuiler]: [$tag], warning by $log")
    }

    fun logW(tag: String, msg: String, log: Throwable) {
        if (detailLog) return
        XposedBridge.log("[W/Cemiuiler]: [$tag] $msg, warning by $log")
    }

    fun logE(tag: String, log: Throwable?, exp: Exception?) {
        val logMessage = "[E/Cemiuiler]: [$tag]" +
            when {
                log != null -> ", hook failed by $log"
                exp != null -> ", hook failed by $exp"
                else -> ""
            }
        XposedBridge.log(logMessage)
    }

    fun logE(tag: String, msg: String, log: Throwable?, exp: Exception?) {
        val logMessage = "[E/Cemiuiler]: [$tag] $msg" +
            when {
                log != null -> ", hook failed by $log"
                exp != null -> ", hook failed by $exp"
                else -> ""
            }
        XposedBridge.log(logMessage)
    }

}
