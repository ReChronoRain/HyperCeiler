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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.api

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull

// by StarVoyager
object LazyClass {
    val clazzMiuiBuild by lazy {
        loadClass("miui.os.Build")
    }

    val AndroidBuildCls by lazy {
        loadClass("android.os.Build")
    }

    val SettingsFeaturesClass by lazy {
        loadClass("com.android.settings.utils.SettingsFeatures")
    }

    val FeatureParserCls by lazy {
        loadClass("miui.util.FeatureParser")
    }

    val SystemProperties by lazy {
        loadClass("android.os.SystemProperties")
    }

    val MiuiBuildCls by lazy {
        loadClass("miui.os.Build")
    }

    val SettingsFeaturesCls by lazy {
        loadClass("com.android.settings.utils.SettingsFeatures")
    }

    val AiasstVisionSystemUtilsCls by lazy {
        loadClass("com.xiaomi.aiasst.vision.utils.SystemUtils")
    }

    val SupportAiSubtitlesUtils by lazy {
        loadClass("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils")
    }

    val MiuiSettingsCls by lazy {
        loadClass("com.android.settings.MiuiSettings")
    }

    val mNewClockClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiStatusBarClock")
    }

    val StrongToast by lazy {
        loadClassOrNull("com.android.systemui.toast.MIUIStrongToast")
    }

    val NewStrongToast by lazy {
        loadClassOrNull("com.miui.toast.MIUIStrongToast")
    }
}
