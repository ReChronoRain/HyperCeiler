package com.sevtinge.hyperceiler.hook.module.hook.aod

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook

object UnlockShortCuts: BaseHook() {
    var list: MutableList<String> = mutableListOf()

    override fun init() {
        loadClass("com.miui.keyguard.shortcuts.utils.DataUtils")
            .methodFinder()
            .filterByName("loadWhiteItems")
            .first()
            .createAfterHook { it ->
                val originalResult = it.result as? List<*> ?: return@createAfterHook
                list.clear()

                originalResult.forEach { item ->
                    if (item is String) {
                        list.add(item)
                    }
                }

                list.add("com.sevtinge.hyperceiler-HYPERCEILER")
                it.result = list
            }
    }

}
