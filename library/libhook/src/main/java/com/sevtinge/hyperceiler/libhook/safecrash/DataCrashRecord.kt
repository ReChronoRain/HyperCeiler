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

import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import org.json.JSONArray
import org.json.JSONObject

/**
 * 崩溃记录实体类
 */
data class DataCrashRecord(
    val pkg: String,
    val time: Long,
    val count: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("p", pkg)
            put("t", time)
            put("c", count)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DataCrashRecord? {
            return try {
                DataCrashRecord(
                    pkg = json.getString("p"),
                    time = json.optLong("t", -1L),
                    count = json.optInt("c", 0)
                )
            } catch (e: Exception) {
                XposedLog.e("CrashRecord", "Parse error", e)
                null
            }
        }

        fun parseList(jsonString: String?): MutableList<DataCrashRecord> {
            val list = mutableListOf<DataCrashRecord>()
            if (jsonString.isNullOrEmpty() || jsonString == "[]") return list

            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    fromJson(array.getJSONObject(i))?.let { list.add(it) }
                }
            } catch (e: Exception) {
                XposedLog.e("CrashRecord", "List parse error", e)
            }
            return list
        }

        fun listToJsonString(list: List<DataCrashRecord>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

