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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool

import android.content.Context
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadFirstClass

object MiuixPreferenceUtils {
    private const val DROP_DOWN_PREFERENCE_CLASS = "miuix.preference.DropDownPreference"
    private const val TEXT_PREFERENCE_CLASS = "miuix.preference.TextPreference"
    private const val ANDROIDX_PREFERENCE_CLASS = "androidx.preference.Preference"

    fun createDropDownPreference(context: Context): Any {
        return loadClass(DROP_DOWN_PREFERENCE_CLASS).newInstance(context)
    }

    fun createTextPreference(context: Context): Any {
        return loadFirstClass(
            TEXT_PREFERENCE_CLASS,
            ANDROIDX_PREFERENCE_CLASS
        ).newInstance(context)
    }

    fun configureDropDownPreference(
        preference: Any,
        title: CharSequence,
        entries: Array<CharSequence>,
        entryValues: Array<CharSequence>,
        value: String,
        visible: Boolean,
        order: Int
    ) {
        preference.apply {
            callMethod("setTitle", title)
            callMethod("setEntries", entries)
            callMethod("setEntryValues", entryValues)
            callMethod("setValue", value)
            callMethod("setVisible", visible)
            callMethod("setOrder", order)
        }
    }

    fun getPreferenceKey(preference: Any): String? {
        return preference.callMethod("getKey") as? String
    }

    fun getPreferences(screen: Any): List<Any> {
        val count = screen.callMethod("getPreferenceCount") as? Int ?: return emptyList()
        return (0 until count).mapNotNull { index -> screen.callMethod("getPreference", index) }
    }

    fun findPreference(fragment: Any, key: String): Any? {
        return fragment.callMethod("findPreference", key)
    }
}
