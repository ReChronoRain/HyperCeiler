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
package com.sevtinge.hyperceiler.module.hook.home.title

import android.util.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.*
import com.sevtinge.hyperceiler.module.hook.home.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.devicesdk.*

class TitleFontSize : HomeBaseHook() {

    override fun initForNewHome() {
        if (mPrefsMap.getInt("home_title_font_size", 12) == 12 &&
            mPrefsMap.getInt("home_drawer_title_font_size", 12) == 12
            ) return

        val appIconClass = Class.forName("com.miui.home.launcher.AppIcon", false, lpparam.classLoader)  // 抽屉

        MethodFinder.fromClass("com.miui.home.launcher.ShortcutIcon").filterByName("updateTitleSize")
            .filterByParamCount(0).first().createAfterHook {
                val shortcutIcon = it.thisObject as TextView

                shortcutIcon.setTextSize(
                    0, DisplayUtils.sp2px(
                        mPrefsMap.getInt(
                            if (appIconClass.isInstance(shortcutIcon)) { // 抽屉
                                "home_drawer_title_font_size"
                            } else { // 桌面
                                "home_title_font_size"
                            }, 12
                        ).toFloat()
                    ).toFloat()
                )

            }

        if (mPrefsMap.getInt("home_title_font_size", 12) == 12) return
        // 文件夹标题
        MethodFinder.fromClass("com.miui.home.launcher.TitleTextView").filterByName("updateSizeOnIconSizeChanged")
            .first().replaceMethod {
                (it.thisObject as TextView).setTextSize(
                    0, DisplayUtils.sp2px(mPrefsMap.getInt("home_title_font_size", 12).toFloat()).toFloat()
                )
            }

        ConstructorFinder.fromClass("com.miui.home.launcher.TitleTextView").first().createAfterHook {
            (it.thisObject as TextView).setTextSize(
                0, DisplayUtils.sp2px(mPrefsMap.getInt("home_title_font_size", 12).toFloat()).toFloat()
            )
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

