package com.sevtinge.cemiuiler.module.home.recent

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.ResourcesHookData
import com.sevtinge.cemiuiler.utils.ResourcesHookMap
import com.sevtinge.cemiuiler.utils.devicesdk.dp2px
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import de.robv.android.xposed.XC_MethodHook

object RecentResource : BaseHook() {
    private val hookMap = ResourcesHookMap<String, ResourcesHookData>()
    private fun hook(param: XC_MethodHook.MethodHookParam) {
        try {
            val resName = appContext.resources.getResourceEntryName(param.args[0] as Int)
            val resType = appContext.resources.getResourceTypeName(param.args[0] as Int)
            if (hookMap.isKeyExist(resName)) if (hookMap[resName]?.type == resType) {
                param.result = hookMap[resName]?.afterValue
            }
        } catch (ignore: Exception) {
        }
    }

    override fun init() {
        Application::class.java.hookBeforeMethod("attach", Context::class.java) { it ->
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            EzXHelper.initAppContext(it.args[0] as Context)

            Resources::class.java.hookBeforeMethod("getBoolean", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimension", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelOffset", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getDimensionPixelSize", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getInteger", Int::class.javaPrimitiveType) { hook(it) }
            Resources::class.java.hookBeforeMethod("getText", Int::class.javaPrimitiveType) { hook(it) }

            val value = mPrefsMap.getInt("task_view_corners", -1).toFloat()
            val value1 = mPrefsMap.getInt("task_view_header_height", -1).toFloat()
            if (value != -1f && value != 20f) {
                hookMap["recents_task_view_rounded_corners_radius_min"] = ResourcesHookData("dimen", dp2px(value))
                hookMap["recents_task_view_rounded_corners_radius_max"] = ResourcesHookData("dimen", dp2px(value))
            }
            if (value1 != -1f && value != 40f) hookMap["recents_task_view_header_height"] =
                ResourcesHookData("dimen", dp2px(value1))
        }
    }

}
