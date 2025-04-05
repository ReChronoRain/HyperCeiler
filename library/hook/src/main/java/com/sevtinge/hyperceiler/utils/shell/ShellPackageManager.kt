package com.sevtinge.hyperceiler.utils.shell

import android.content.ComponentName
import android.util.Log
import com.sevtinge.hyperceiler.hook.BuildConfig

@Suppress("unused")
object ShellPackageManager {
    private const val TAG = "ShellPackageManager"
    private const val PM = "pm"

    private val DEBUG = BuildConfig.DEBUG

    @JvmStatic
    fun enable(packageName: String): Boolean {
        return enableOrDisable(packageName, true)
    }

    @JvmStatic
    fun enable(componentName: ComponentName): Boolean {
        return enableOrDisable(componentName.flattenToString(), true)
    }

    @JvmStatic
    fun disable(packageName: String): Boolean {
        return enableOrDisable(packageName, false)
    }

    @JvmStatic
    fun disable(componentName: ComponentName): Boolean {
        return enableOrDisable(componentName.flattenToString(), false)
    }

    // TODO: --user [USER_ID] 指定特定用户
    @JvmStatic
    fun enableOrDisable(packageName: String, isEnable: Boolean): Boolean {
        val status = if (isEnable) {
            "enable"
        } else {
            "disable"
        }

        val commandResult = ShellUtils.execCommand("$PM $status $packageName", true)
        if (DEBUG) {
            Log.d(TAG, commandResult.toString())
        }
        return commandResult.result == 0
    }

    @JvmStatic
    fun enableOrDisable(componentName: ComponentName, isEnable: Boolean): Boolean {
        return enableOrDisable(componentName.flattenToString(), isEnable)
    }
}
