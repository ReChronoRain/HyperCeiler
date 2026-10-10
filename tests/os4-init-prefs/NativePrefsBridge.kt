package com.sevtinge.hyperceiler.common.utils
import android.content.SharedPreferences
object PrefsBridge {
    var preferences: SharedPreferences? = null
    val values = mutableMapOf<String, Any>()
    var rejectRead = false
    @Volatile var onGetPreferences: (() -> Unit)? = null
    fun getSharedPreferences(): SharedPreferences? { onGetPreferences?.invoke(); return preferences }
    private fun ready() { check(preferences != null && !rejectRead) { "preferences unavailable" } }
    fun getBoolean(key: String): Boolean { ready(); return values[key] as? Boolean ?: false }
    fun getStringAsInt(key: String, defaultValue: Int): Int { ready(); return values[key] as? Int ?: defaultValue }
}
