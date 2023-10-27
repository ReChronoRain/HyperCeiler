package com.sevtinge.hyperceiler.utils.log

import com.sevtinge.hyperceiler.BuildConfig
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object XposedLogUtilsKt {
    private val isDebugVersion = BuildConfig.BUILD_TYPE.contains("debug")
    private val isNotReleaseVersion = !BuildConfig.BUILD_TYPE.contains("release")
    private val detailLog = BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log")

    // Xposed debug 日志输出
    fun logI(tag: String?, msg: String) {
        if (!isDebugVersion) return
        if (detailLog) return
        if (tag != null) XposedBridge.log("[HyperCeiler][I][$tag]: $msg") else XposedBridge.log("[HyperCeiler][I]: $msg")

    }

    fun logW(tag: String, msg: String) {
        if (detailLog) return
        XposedBridge.log("[HyperCeiler][W][$tag]: $msg")
    }

    fun logW(tag: String, log: Throwable) {
        if (detailLog) return
        XposedBridge.log("[HyperCeiler][W][$tag]: $log")
    }

    fun logW(tag: String, msg: String, log: Throwable) {
        if (detailLog) return
        XposedBridge.log("[HyperCeiler][W][$tag]: $msg, warning by $log")
    }

    fun logE(tag: String, log: Throwable?, exp: Exception?) {
        val logMessage = "[HyperCeiler][E][$tag]: " +
            when {
                log != null -> ", hook failed by $log"
                exp != null -> ", hook failed by $exp"
                else -> ""
            }
        XposedBridge.log(logMessage)
    }

    fun logE(tag: String, msg: String, log: Throwable?, exp: Exception?) {
        val logMessage = "[HyperCeiler][E][$tag]: $msg" +
            when {
                log != null -> ", hook failed by $log"
                exp != null -> ", hook failed by $exp"
                else -> ""
            }
        XposedBridge.log(logMessage)
    }

    fun logD(tag: String, msg: String) {
        if (!isDebugVersion) return
        XposedBridge.log("[HyperCeiler][D][$tag]: $msg")
    }

    fun logD(msg: String) {
        if (!isDebugVersion) return
        XposedBridge.log("[HyperCeiler][D]: $msg")
    }

}
