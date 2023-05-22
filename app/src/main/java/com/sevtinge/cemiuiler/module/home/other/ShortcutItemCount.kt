package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callMethod

object ShortcutItemCount : BaseHook() {
    override fun init() {

        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxCountInCurrentOrientation" }.hookAfter {
            it.result = 20
        }
        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxShortcutItemCount" }.hookAfter {
            it.result = 20
        }
        findMethod("com.miui.home.launcher.shortcuts.AppShortcutMenu") { name == "getMaxVisualHeight" }.hookAfter {
            it.result = it.thisObject.callMethod("getItemHeight")
        }

        //我的摆烂式写法，在 new bing 面前不堪一击，呜呜呜~
        /*findAndHookMethod(
            "com.miui.home.launcher.shortcuts.AppShortcutMenu",
            "getMaxCountInCurrentOrientation",
            object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    if (param != null) {
                        param.result = 20
                    }
                }
            })

        findAndHookMethod(
            "com.miui.home.launcher.shortcuts.AppShortcutMenu",
            "getMaxShortcutItemCount",
            object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    if (param != null) {
                      param.result = 20
                    }
                }
            })

        findAndHookMethod(
            "com.miui.home.launcher.shortcuts.AppShortcutMenu",
            "getMaxVisualHeight",
            object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    if (param != null) {
                       param.result = param.thisObject.callMethod("getItemHeight")
                    }
                }
            })*/

        // New bing 写的精简，自行评估
        /* val methods = arrayOf(
             "getMaxCountInCurrentOrientation" to 20,
             "getMaxShortcutItemCount" to 20,
             "getMaxVisualHeight" to { param: MethodHookParam ->
                 param.thisObject.callMethod("getItemHeight")
             }
         )

         for ((methodName, result) in methods) {
             findAndHookMethod(
                 "com.miui.home.launcher.shortcuts.AppShortcutMenu",
                 methodName,
                 object : MethodHook() {
                     override fun after(param: MethodHookParam?) {
                         if (param != null) {
                             param.result = when (result) {
                                 is Int -> result
                                 is Function1<*, *> -> result(param as Nothing)
                                 else -> null
                             }
                         }
                     }
                 }
             )
         }*/

    }
}