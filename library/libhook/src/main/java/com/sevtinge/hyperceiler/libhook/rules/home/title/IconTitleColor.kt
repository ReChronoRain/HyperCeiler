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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.beforeHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge

object IconTitleColor : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        val color = PrefsBridge.getInt("home_title_title_color", -1)

        findClass("com.miui.home.launcher.ShortcutIcon").beforeHookMethod("getTextColor") { param ->
            param.result = color
        }

        findClass("com.miui.home.launcher.ShortcutIcon").afterHookMethod("onFinishInflate") { param ->
            (param.thisObject as TextView).setTextColor(color)
        }

        runCatching {
            findClass("com.miui.home.common.utils.TextViewUtils").afterHookMethod(
                "adaptTitleStyleToWallpaper"
            ) { param ->
                val mTitle = param.args[1] as TextView
                if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextColor(color)
                }
            }
        }.onFailure {
            findClass("com.miui.home.launcher.common.Utilities").afterHookMethod(
                "adaptTitleStyleToWallpaper"
            ) { param ->
                val mTitle = param.args[1] as TextView
                if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextColor(color)
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun initBase() {
        runCatching {
            initForNewHome()
        }.onFailure {
            oldHook()
        }

    }

    private fun oldHook() {
        val value = PrefsBridge.getInt("home_title_title_color", -1)
        val launcherClass = findClass("com.miui.home.launcher.Launcher")
        val shortcutInfoClass = findClass("com.miui.home.launcher.ShortcutInfo")
        if (value == -1) return

        findClass("com.miui.home.launcher.ItemIcon").afterHookMethod(
            "onFinishInflate"
        ) {
            val mTitle = it.thisObject.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.maml.MaMlWidgetView").afterHookMethod(
            "onFinishInflate"
        ) {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.LauncherMtzGadgetView").afterHookMethod(
            "onFinishInflate"
        ) {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.LauncherWidgetView").afterHookMethod(
            "onFinishInflate"
        ) {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.ShortcutIcon").afterHookMethod(
            "fromXml",
            Int::class.java,
            launcherClass,
            ViewGroup::class.java,
            shortcutInfoClass
        ) {
            val buddyIconView =
                it.args[3]!!.callMethod("getBuddyIconView", it.args[2]) as View
            val mTitle = buddyIconView.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.ShortcutIcon").afterHookMethod(
            "createShortcutIcon",
            Int::class.java,
            launcherClass,
            ViewGroup::class.java
        ) {
            val buddyIcon = it.result as View
            val mTitle = buddyIcon.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.common.Utilities").afterHookMethod(
            "adaptTitleStyleToWallpaper",
            Context::class.java,
            TextView::class.java,
            Int::class.java,
            Int::class.java
        ) {
            val mTitle = it.args[1] as TextView
            if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                mTitle.setTextColor(value)
            }
        }

    }
}
