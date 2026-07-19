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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.view.Menu
import android.view.MenuItem
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Methods
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.appContext
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.initAppContext
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@SuppressLint("DiscouragedApi")
object AddAppInfoEntry : BaseHook() {
    //override val key = "add_aosp_app_info_entry"
    private val idIdMiuixActionEndMenuGroup by lazy {
        appContext.resources.getIdentifier("miuix_action_end_menu_group", "id", EzXposed.packageName)
    }
    private val idDrawableIconSettings by lazy {
        appContext.resources.getIdentifier("icon_settings", "drawable", EzXposed.packageName)
    }
    private val idStringAppManagerAppInfoLabel by lazy {
        appContext.resources.getIdentifier("app_manager_app_info_label", "string", EzXposed.packageName)
    }

    override fun init() {
        val clazzApplicationsDetailsActivity =
            loadClass("com.miui.appmanager.ApplicationsDetailsActivity")
        clazzApplicationsDetailsActivity.findMethod { name("onCreateOptionsMenu") }
            .createHook {
                after {
                    val activity = it.thisObject as Activity
                    initAppContext(activity, true)
                    val pkgName = activity.intent.getStringExtra("package_name")!!
                    val myUserId =
                        Methods.callStaticMethod(UserHandle::class.java, "myUserId") as Int
                    val uid = activity.intent.getIntExtra("miui.intent.extra.USER_ID", myUserId)
                    val menuItem = (it.args[0] as Menu).add(
                        idIdMiuixActionEndMenuGroup, 0, 0, R.string.security_center_aosp_app_info_label
                    )
                    menuItem.intent = Intent(Intent.ACTION_MAIN).apply {
                        val bundle = Bundle().apply {
                            putString("package", pkgName)
                            putInt("uid", uid)
                        }
                        val stringAppManagerAppInfoLabel =
                            activity.getString(idStringAppManagerAppInfoLabel)
                        setClassName(
                            "com.android.settings",
                            "com.android.settings.SubSettings"
                        )
                        putExtra(
                            ":settings:show_fragment",
                            "com.android.settings.applications.appinfo.AppInfoDashboardFragment"
                        )
                        putExtra(
                            ":settings:show_fragment_title",
                            stringAppManagerAppInfoLabel
                        )
                        putExtra(
                            ":settings:show_fragment_args",
                            bundle
                        )
                    }
                    menuItem.setIcon(idDrawableIconSettings)
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }
    }
}
