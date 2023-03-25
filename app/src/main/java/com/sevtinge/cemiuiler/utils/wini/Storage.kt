package com.sevtinge.cemiuiler.utils.wini

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.sevtinge.cemiuiler.module.wini.model.ConfigModel

object Storage {
    const val DATA_FILENAME = "WINIConfig"
    const val CONFIG_JSON = "config"

    @SuppressLint("WorldWriteableFiles")
    fun saveData(key: String, value: String, context: Context) {
        try {
            val sharedPreferences =
                context.getSharedPreferences(DATA_FILENAME, Context.MODE_WORLD_WRITEABLE)
            val editor = sharedPreferences.edit()
            editor.putString(key, value)
            editor.apply()
        } catch (e: Throwable) {
            // 也许是模块尚未加载
        }
    }

    @SuppressLint("WorldReadableFiles")
    fun getData(key: String, defaultValue: String, context: Context): String {
        try {
            val sharedPreferences =
                context.getSharedPreferences(DATA_FILENAME, Context.MODE_WORLD_READABLE)
            return sharedPreferences.getString(key, defaultValue) ?: return defaultValue
        } catch (e: Throwable) {
            // 也许是模块尚未加载
        }
        return defaultValue
    }

    fun getConfig(context: Context): ConfigModel {
        val configJsonString = getData(CONFIG_JSON, "{\"versionCode\":0}", context)
        return getConfig(configJsonString)
    }

    fun getConfig(configJsonString: String): ConfigModel {
        return Gson().fromJson(configJsonString, ConfigModel::class.java)
    }

    fun saveConfig(config: ConfigModel, context: Context) {
        val configString = Gson().toJson(config)
        saveData(CONFIG_JSON, configString, context)
    }
}