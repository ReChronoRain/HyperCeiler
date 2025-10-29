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
package com.sevtinge.hyperceiler.hook.module.rules.home.title

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder

object IconTitleColor : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        val color = mPrefsMap.getInt("home_title_title_color", -1)

        "com.miui.home.launcher.ShortcutIcon".hookBeforeMethod("getTextColor") { param ->
            param.result = color
        }

        "com.miui.home.launcher.ShortcutIcon".hookAfterMethod("onFinishInflate") { param ->
            (param.thisObject as TextView).setTextColor(color)
        }

        runCatching {
            "com.miui.home.common.utils.TextViewUtils".findClass().methodFinder()
                .filterByName("adaptTitleStyleToWallpaper")
                .first()
                .hookAfterMethod { param ->
                    val mTitle = param.args[1] as TextView
                    if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                        mTitle.setTextColor(color)
                    }
                }
        }.onFailure {
            "com.miui.home.launcher.common.Utilities".findClass().methodFinder()
                .filterByName("adaptTitleStyleToWallpaper")
                .first()
                .hookAfterMethod { param ->
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
        val value = mPrefsMap.getInt("home_title_title_color", -1)
        val launcherClass = "com.miui.home.launcher.Launcher".findClass()
        val shortcutInfoClass = "com.miui.home.launcher.ShortcutInfo".findClass()
        if (value == -1) return

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
            "fromXml",
            Int::class.javaPrimitiveType,
            launcherClass,
            ViewGroup::class.java,
            shortcutInfoClass
        ) {
            val buddyIconView =
                it.args[3].callMethod("getBuddyIconView", it.args[2]) as View
            val mTitle = buddyIconView.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        "com.miui.home.launcher.ShortcutIcon".hookAfterMethod(
            "createShortcutIcon",
            Int::class.javaPrimitiveType,
            launcherClass,
            ViewGroup::class.java
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

    }
}
