/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker.systemui

import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.sevtinge.hyperceiler.core.R
import com.sevtinge.hyperceiler.dashboard.DashboardFragment
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import fan.appcompat.app.AlertDialog

/**
 * Navigation settings fragment for SystemUI.
 */
class NavigationSettings : DashboardFragment() {
    var navigation: SwitchPreference? = null

    override fun getPreferenceScreenResId(): Int {
        return R.xml.system_ui_navigation
    }

    override fun initPrefs() {
        navigation = findPreference("prefs_key_system_ui_hide_navigation_bar")
        navigation?.setOnPreferenceChangeListener { preference: Preference, newValue: Any? ->
            // 询问是否立即重启
            val systemUiName = getString(R.string.system_ui)
            val homeName = getString(R.string.mihome)
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.navigation_restart_confirm_title)
                .setMessage(getString(R.string.navigation_restart_confirm_message,
                                      systemUiName,
                                      homeName))
                .setPositiveButton(R.string.navigation_restart_confirm_ok) { _, _ ->
                    // 重启“系统界面”和“系统桌面”
                    AppsTool.killApps("com.miui.home", "com.android.systemui")
                }.setNegativeButton(android.R.string.cancel, null).show()
            true
        }
    }
}
