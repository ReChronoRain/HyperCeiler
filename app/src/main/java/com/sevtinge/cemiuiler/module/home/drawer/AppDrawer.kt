package com.sevtinge.cemiuiler.module.home.drawer

import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.callMethodAs
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectFieldAs
import com.sevtinge.cemiuiler.utils.woobox.hookAfterMethod

object AppDrawer : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("home_drawer_all")) {
            try {
                findMethod("com.miui.home.launcher.allapps.category.BaseAllAppsCategoryListContainer") {
                    name == "buildSortCategoryList"
                }
            } catch (e: Exception) {
                findMethod("com.miui.home.launcher.allapps.category.AllAppsCategoryListContainer") {
                    name == "buildSortCategoryList"
                }
            }.hookAfter {
                val list = it.result as ArrayList<*>
                if (list.size > 1) {
                    list.removeAt(0)
                    it.result = list
                }
            }
        }

        if (mPrefsMap.getBoolean("home_drawer_editor")) {
            "com.miui.home.launcher.allapps.AllAppsGridAdapter".hookAfterMethod(
                "onBindViewHolder", "com.miui.home.launcher.allapps.AllAppsGridAdapter.ViewHolder".findClass(), Int::class.javaPrimitiveType
            ) {
                if (it.args[0].callMethodAs<Int>("getItemViewType") == 64) {
                    it.args[0].getObjectFieldAs<View>("itemView").visibility = View.INVISIBLE
                }
            }
        }

    }
}