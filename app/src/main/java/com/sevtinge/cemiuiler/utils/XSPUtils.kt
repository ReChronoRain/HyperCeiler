package com.sevtinge.cemiuiler.utils

import com.sevtinge.cemiuiler.BuildConfig
import de.robv.android.xposed.XSharedPreferences

object XSPUtils {
    private var prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, "cemiuiler_config")

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getBoolean(key, defValue)
    }

    fun getInt(key: String, defValue: Int): Int {
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getInt(key, defValue)
    }

    fun getFloat(key: String, defValue: Float): Float {
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getFloat(key, defValue)
    }

    fun getString(key: String, defValue: String): String? {
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getString(key, defValue)
    }
}