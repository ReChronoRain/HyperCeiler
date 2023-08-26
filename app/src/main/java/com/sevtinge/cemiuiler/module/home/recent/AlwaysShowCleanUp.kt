package com.sevtinge.cemiuiler.module.home.recent

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.isPad

class AlwaysShowCleanUp: BaseHook() {
    override fun init() {
        if (isPad()){
            // 平板设备
            hookAllMethods(
                "com.miui.home.recents.views.RecentsDecorations",
                "updateClearContainerVisible",
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        param.result = true
                    }
                })
        }else{
            // 手机设备
            hookAllMethods(
                "com.miui.home.recents.views.RecentsContainer",
                "updateClearContainerVisible",
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        param.result = true
                    }
                })
        }
    }
}
