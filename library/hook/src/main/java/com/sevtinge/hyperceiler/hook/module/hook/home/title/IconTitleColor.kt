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

import android.annotation.*
import android.content.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.hook.module.hook.home.HomeBaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod

object IconTitleColor : HomeBaseHook() {
    override fun initForNewHome() {
        val color = mPrefsMap.getInt("home_title_title_color", -1)
        if (color == -1) return

        "com.miui.home.launcher.ShortcutIcon".hookBeforeMethod("getTextColor") { param ->
            param.result = color
        }

        "com.miui.home.launcher.ShortcutIcon".hookAfterMethod("onFinishInflate") { param ->
            (param.thisObject as TextView).setTextColor(color)
        }

        "com.miui.home.launcher.common.Utilities".hookAfterMethod(
            "adaptTitleStyleToWallpaper",
            Context::class.java,
            TextView::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ) {
            val mTitle = it.args[1] as TextView
            if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                mTitle.setTextColor(color)
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun initForHomeLower9777() {

        val value = mPrefsMap.getInt("home_title_title_color", -1)
        val launcherClass = "com.miui.home.launcher.Launcher".findClass()
        val shortcutInfoClass = "com.miui.home.launcher.ShortcutInfo".findClass()
        if (value == -1) return
        try {
            "com.miui.home.launcher.ItemIcon".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.maml.MaMlWidgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.LauncherMtzGadgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.LauncherWidgetView".hookAfterMethod(
                "onFinishInflate"
            ) {
                val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.ShortcutIcon".hookAfterMethod(
                "fromXml", Int::class.javaPrimitiveType, launcherClass, ViewGroup::class.java, shortcutInfoClass
            ) {
                val buddyIconView = it.args[3].callMethod("getBuddyIconView", it.args[2]) as View
                val mTitle = buddyIconView.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.ShortcutIcon".hookAfterMethod(
                "createShortcutIcon", Int::class.javaPrimitiveType, launcherClass, ViewGroup::class.java
            ) {
                val buddyIcon = it.result as View
                val mTitle = buddyIcon.getObjectField("mTitle") as TextView
                mTitle.setTextColor(value)
            }
            "com.miui.home.launcher.common.Utilities".hookAfterMethod(
                "adaptTitleStyleToWallpaper",
                Context::class.java,
                TextView::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ) {
                val mTitle = it.args[1] as TextView
                if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextColor(value)
                }
            }
        } catch (e: Throwable) {
            Log.ex(e)
        }

    }
}
