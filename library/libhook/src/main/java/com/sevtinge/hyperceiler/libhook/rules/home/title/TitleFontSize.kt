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
package com.sevtinge.hyperceiler.libhook.rules.home.title

import android.util.TypedValue
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookConstructor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.replaceMethod
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

class TitleFontSize : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        val desktopSp = PrefsBridge.getInt("home_title_font_size", 12).toFloat()
        val drawerSp = PrefsBridge.getInt("home_drawer_title_font_size", 12).toFloat()
        if (desktopSp == 12f && drawerSp == 12f) {
            XposedLog.d(TAG, "No need to be hooked")
            return
        }

        val defaultSizePx by lazy {  // 必须在 hooker 内被 call，DeviceConfig 依赖 Context
            findClass("com.miui.home.launcher.DeviceConfig").callStaticMethod("getIconTitleTextSize") as Float
        }

        val appIconClass =
            findClass("com.miui.home.launcher.AppIcon", lpparam.classLoader)  // 抽屉

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
        // Todo: 堆叠桌面版本不存在此类,需要修复
        runCatching {
            findClass("com.miui.home.launcher.TitleTextView").replaceMethod("updateSizeOnIconSizeChanged") {
                (it.thisObject as TextView).textSize = desktopSp
            }

            findClass("com.miui.home.launcher.TitleTextView").afterHookConstructor {
                (it.thisObject as TextView).textSize = desktopSp
            }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "TitleFontSize failed", it)
        }
    }

    override fun initBase() {
        runCatching {
            initForNewHome()
        }.onFailure {
            initForHomeLower9777()
        }
    }

    private fun initForHomeLower9777() {
        if (PrefsBridge.getInt("home_title_font_size", 12) == 12) return

        findClass("com.miui.home.launcher.common.Utilities")
            .afterHookMethod("adaptTitleStyleToWallpaper") { param ->
                val mTitle = param.args[1] as? TextView
                if (mTitle != null && mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        PrefsBridge.getInt("home_title_font_size", 12).toFloat()
                    )
                }
            }
    }
}

