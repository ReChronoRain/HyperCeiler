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
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.get
import androidx.core.view.size
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.initAppContext
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

@SuppressLint("DiscouragedApi")
object AddAppManagerEntry : BaseHook() {
    private const val RES_ENTRY_LABEL = "security_center_aosp_app_manager_label"
    private const val ACTIVITY_APP_MANAGER = "com.miui.appmanager.AppManagerMainActivity"
    private const val ACTIVITY_APPCOMPAT = "miuix.appcompat.app.AppCompatActivity"

    private val idMenuAospAppManager by lazy {
        getFakeResId("security_center_aosp_app_manager_menu")
    }

    override fun init() {
        findClassIfExists(ACTIVITY_APPCOMPAT)?.findMethod {
            name("onOptionsMenuViewAdded")
            parameterTypes(Menu::class.java, Menu::class.java)
        }?.createAfterHook {
            val activity = it.thisObject as? Activity ?: return@createAfterHook
            if (activity.javaClass.name != ACTIVITY_APP_MANAGER) return@createAfterHook
            val endMenu = it.args[1] as? Menu ?: return@createAfterHook

            val label = getModuleString(activity, RES_ENTRY_LABEL)
            val groupId = getActionEndMenuGroupId(activity)

            initAppContext(activity, false)

            if (endMenu.size == 0 || endMenu.hasAppManagerEntry(label)) return@createAfterHook

            addAppManagerEntry(activity, endMenu, groupId, label)
        }
    }

    private fun addAppManagerEntry(
        activity: Activity,
        menu: Menu,
        groupId: Int,
        label: String
    ) {
        menu.add(groupId, idMenuAospAppManager, Menu.NONE, label).apply {
            intent = createAppManagerIntent()
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            setOnMenuItemClickListener {
                activity.startActivity(createAppManagerIntent())
                true
            }
        }
    }

    private fun getModuleString(context: Context, name: String): String {
        val res = getModuleResources(context)
        return res.getString(res.getIdentifier(name, "string", ProjectApi.mAppModulePkg))
    }

    private fun getModuleResources(context: Context): Resources =
        context.packageManager.getResourcesForApplication(ProjectApi.mAppModulePkg)

    private fun createAppManagerIntent(): Intent =
        Intent(Intent.ACTION_MAIN).setClassName(
            "com.android.settings",
            "com.android.settings.applications.ManageApplications"
        )

    private fun getActionEndMenuGroupId(context: Context): Int =
        context.resources.getIdentifier(
            "miuix_action_end_menu_group",
            "id",
            context.packageName
        )

    private fun Menu.hasAppManagerEntry(label: String): Boolean {
        val targetComponent = createAppManagerIntent().component
        return (0 until size).any { index ->
            val item = this[index]
            item.itemId == idMenuAospAppManager ||
                item.title?.toString() == label ||
                item.intent?.component == targetComponent
        }
    }
}
