package com.sevtinge.hyperceiler

import android.annotation.SuppressLint
import android.content.Context
import com.sevtinge.hyperceiler.module.base.BaseXposedInit.isSafeModeOn
import de.robv.android.xposed.XposedBridge

@SuppressLint("StaticFieldLeak")
object CrashRecordKt : Thread.UncaughtExceptionHandler {

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null

    fun init(context: Context) {
        mContext = context
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        if (BuildConfig.DEBUG) XposedBridge.log("[HyperCeiler][I]: CrashRecord Loaded")
    }

    override fun uncaughtException(p0: Thread, p1: Throwable) {
        XposedBridge.log("[HyperCeiler][W]: Crash happened")
        mContext?.let {
            val pref = it.createDeviceProtectedStorageContext().getSharedPreferences("Crash_Handler", Context.MODE_PRIVATE)
            if (BuildConfig.DEBUG) {
                XposedBridge.log("${System.currentTimeMillis()}")
                XposedBridge.log("${pref.getLong("last_time", 0L)}")
                XposedBridge.log("${System.currentTimeMillis() - pref.getLong("last_time", 0L)}")
            }
            if (System.currentTimeMillis() - pref.getLong("last_time", 0L) < 60 * 1000L) {
                XposedBridge.log("[HyperCeiler][W]: Crash happened again in one minute")
                if (pref.getInt("times", 0) >= 3) {
                    isSafeModeOn = true
                    XposedBridge.log("[HyperCeiler][W]: More than 3 times, clear MODULE_CONFIG")
                    pref.edit().putInt("times", 0).apply()
                }
                pref.edit().putInt("times", pref.getInt("times", 0) + 1).apply()
            }
            pref.edit().putLong("last_time", System.currentTimeMillis()).apply()
            Thread.sleep(500)
        }
        mDefaultHandler?.uncaughtException(p0, p1)
    }
}
