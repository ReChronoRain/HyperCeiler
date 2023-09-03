package com.sevtinge.cemiuiler.module.hook.home.drawer

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethodAs
import com.sevtinge.cemiuiler.utils.findClass
import com.sevtinge.cemiuiler.utils.getObjectFieldAs
import com.sevtinge.cemiuiler.utils.hookAfterMethod

object AppDrawer : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("home_drawer_all")) {
            try {
                loadClassOrNull("com.miui.home.launcher.allapps.category.BaseAllAppsCategoryListContainer")!!
                    .methodFinder()
                    .first {
                        name == "buildSortCategoryList"
                    }
            } catch (e: Exception) {
                loadClass("com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer").methodFinder()
                    .first {
                        name == "buildSortCategoryList"
                    }
            }.createHook {
                after {
                    val list = it.result as ArrayList<*>
                    if (list.size > 1) {
                        list.removeAt(0)
                        it.result = list
                    }
                }
            }
        }

        if (mPrefsMap.getBoolean("home_drawer_editor")) {
            "com.miui.home.launcher.allapps.AllAppsGridAdapter".hookAfterMethod(
                "onBindViewHolder",
                "com.miui.home.launcher.allapps.AllAppsGridAdapter.ViewHolder".findClass(),
                Int::class.javaPrimitiveType
            ) {
                if (it.args[0].callMethodAs<Int>("getItemViewType") == 64) {
                    it.args[0].getObjectFieldAs<View>("itemView").visibility = View.INVISIBLE
                }
            }
        }

    }
}
