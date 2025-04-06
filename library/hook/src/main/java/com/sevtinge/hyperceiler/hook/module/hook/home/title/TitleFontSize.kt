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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.home.title

import android.util.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.*
import com.sevtinge.hyperceiler.hook.module.hook.home.HomeBaseHook
import com.sevtinge.hyperceiler.hook.utils.replaceMethod

class TitleFontSize : HomeBaseHook() {

    override fun initForNewHome() {
        val desktopSp = mPrefsMap.getInt("home_title_font_size", 12).toFloat()
        val drawerSp = mPrefsMap.getInt("home_drawer_title_font_size", 12).toFloat()
        if (desktopSp == 12f && drawerSp == 12f) {
            logI(TAG, "No need to be hooked")
            return
        }

        val defaultSizePx by lazy {  // 必须在hooker内被call，DeviceConfig依赖Context
            MethodFinder.fromClass("com.miui.home.launcher.DeviceConfig").filterStatic()
                .filterByName("getIconTitleTextSize").first().invoke(null) as Float
        }

        val appIconClass = Class.forName("com.miui.home.launcher.AppIcon", false, lpparam.classLoader)  // 抽屉

        MethodFinder.fromClass("com.miui.home.launcher.ShortcutIcon").filterByName("onMeasure").first().createHook {
            before {
                (it.thisObject as TextView).setTextSize(0, defaultSizePx)
            }
            after {
                with((it.thisObject as TextView)) {
                    textSize = if (appIconClass.isInstance(this)) drawerSp else desktopSp
                }
            }
        }

        if (desktopSp == 12f) return
        // 文件夹标题
        MethodFinder.fromClass("com.miui.home.launcher.TitleTextView").filterByName("updateSizeOnIconSizeChanged")
            .first().replaceMethod {
                (it.thisObject as TextView).textSize = desktopSp
            }

        ConstructorFinder.fromClass("com.miui.home.launcher.TitleTextView").first().createAfterHook {
            (it.thisObject as TextView).textSize = desktopSp
        }
    }

    override fun initForHomeLower9777() {
        if (mPrefsMap.getInt("home_title_font_size", 12) == 12) return

        MethodFinder.fromClass("com.miui.home.launcher.common.Utilities").filterByName("adaptTitleStyleToWallpaper")
            .first().createAfterHook { param ->
                val mTitle = param.args[1] as? TextView
                if (mTitle != null && mTitle.id == mTitle.resources.getIdentifier(
                        "icon_title", "id", "com.miui.home"
                    )
                ) {
                    mTitle.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP, mPrefsMap.getInt("home_title_font_size", 12).toFloat()
                    )
                }
            }
    }
}

