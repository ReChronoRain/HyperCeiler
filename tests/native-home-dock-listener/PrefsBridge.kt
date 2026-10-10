package com.sevtinge.hyperceiler.common.utils
import android.content.SharedPreferences
object PrefsBridge {
    var preferences: SharedPreferences? = null
    var enabled = false
    val reads = mutableListOf<Pair<String, Boolean>>()
    fun getSharedPreferences() = preferences
    fun getBoolean(key: String): Boolean { reads += key to enabled; return enabled }
    fun getStringAsInt(key: String, defaultValue: Int) = defaultValue
}
