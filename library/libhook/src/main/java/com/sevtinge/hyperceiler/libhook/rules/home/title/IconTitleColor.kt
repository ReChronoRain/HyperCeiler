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
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object IconTitleColor : HomeBaseHookNew() {

    @Version(isPad = false, min = 600000000)
    private fun initForNewHome() {
        val color = PrefsBridge.getInt("home_title_title_color", -1)

        findClass("com.miui.home.launcher.ShortcutIcon").findMethod {
            name("getTextColor")
        }.createBeforeHook { param ->
            param.result = color
        }

        findClass("com.miui.home.launcher.ShortcutIcon").findMethod {
            name("onFinishInflate")
        }.createAfterHook { param ->
            (param.thisObject as TextView).setTextColor(color)
        }

        runCatching {
            findClass("com.miui.home.common.utils.TextViewUtils").findMethod {
                name("adaptTitleStyleToWallpaper")
            }.createAfterHook { param ->
                val mTitle = param.args[1] as TextView
                if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextColor(color)
                }
            }
        }.onFailure {
            findClass("com.miui.home.launcher.common.Utilities").findMethod {
                name("adaptTitleStyleToWallpaper")
            }.createAfterHook { param ->
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

        findClass("com.miui.home.launcher.ItemIcon").findMethod {
            name("onFinishInflate")
        }.createAfterHook {
            val mTitle = it.thisObject.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.maml.MaMlWidgetView").findMethod {
            name("onFinishInflate")
        }.createAfterHook {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.LauncherMtzGadgetView").findMethod {
            name("onFinishInflate")
        }.createAfterHook {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.LauncherWidgetView").findMethod {
            name("onFinishInflate")
        }.createAfterHook {
            val mTitle = it.thisObject.getObjectField("mTitleTextView") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.ShortcutIcon").findMethod {
            name("fromXml")
            parameterTypes(Int::class.javaPrimitiveType!!, launcherClass, ViewGroup::class.java, shortcutInfoClass)
        }.createAfterHook {
            val buddyIconView =
                it.args[3]!!.callMethod("getBuddyIconView", it.args[2]) as View
            val mTitle = buddyIconView.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.ShortcutIcon").findMethod {
            name("createShortcutIcon")
            parameterTypes(Int::class.javaPrimitiveType!!, launcherClass, ViewGroup::class.java)
        }.createAfterHook {
            val buddyIcon = it.result as View
            val mTitle = buddyIcon.getObjectField("mTitle") as TextView
            mTitle.setTextColor(value)
        }
        findClass("com.miui.home.launcher.common.Utilities").findMethod {
            name("adaptTitleStyleToWallpaper")
            parameterTypes(Context::class.java, TextView::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
        }.createAfterHook {
            val mTitle = it.args[1] as TextView
            if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                mTitle.setTextColor(value)
            }
        }

    }
}
