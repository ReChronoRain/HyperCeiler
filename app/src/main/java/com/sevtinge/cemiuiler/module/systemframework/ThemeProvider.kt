package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import miui.drm.DrmManager
import miui.drm.ThemeReceiver

class ThemeProvider : BaseHook() {
    override fun init() {
        var hook: List<XC_MethodHook.Unhook>? = null
        try {
            findMethod(ThemeReceiver::class.java) {
                name == "validateTheme"
            }.hookMethod {
                before {
                    hook = findAllMethods(DrmManager::class.java) {
                        name == "isLegal"
                    }.hookBefore {
                        it.result = DrmManager.DrmResult.DRM_SUCCESS
                    }
                }
                after {
                    hook?.unhookAll()
                }
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}