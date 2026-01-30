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
package com.sevtinge.hyperceiler.libhook.utils.pkg

import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils

object DebugModeUtils {

    /**
     * 获取指定包名的调试版本号
     */
    fun getChooseResult(pkg: String): Int {
        return PrefsUtils.mPrefsMap.getInt("debug_choose_$pkg", 0)
    }

    /**
     * 手动设置指定包名的调试版本号
     */
    fun setChooseResult(pkg: String, isModified: Int) {
        clearChooseResult(pkg)
        PrefsUtils.editor().putInt("prefs_key_debug_choose_$pkg", isModified).commit()
    }

    /**
     * 清除指定包名的调试版本号
     */
    fun clearChooseResult(pkg: String) {
        PrefsUtils.editor().remove("prefs_key_debug_choose_$pkg").commit()
    }

}
