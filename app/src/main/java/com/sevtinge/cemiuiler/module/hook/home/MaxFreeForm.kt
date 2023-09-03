package com.sevtinge.cemiuiler.module.hook.home

import android.util.ArraySet
import com.github.kyuubiran.ezxhelper.ClassUtils
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class MaxFreeForm : BaseHook() {
    override fun init() {
        // CanTaskEnterSmallWindow
        val clazzRecentsAndFSGestureUtils =
            loadClass("com.miui.home.launcher.RecentsAndFSGestureUtils")
        clazzRecentsAndFSGestureUtils.methodFinder().filter {
            name == "canTaskEnterSmallWindow"
        }.toList().createHooks {
            returnConstant(true)
        }

        // CanTaskEnterMiniSmallWindow
        clazzRecentsAndFSGestureUtils.methodFinder().filter {
            name == "canTaskEnterMiniSmallWindow"
        }.toList().createHooks {
            before {
                it.result = ClassUtils.invokeStaticMethodBestMatch(
                    loadClass("com.miui.home.smallwindow.SmallWindowStateHelper"),
                    "getInstance"
                )!!.objectHelper()
                    .invokeMethodBestMatch("canEnterMiniSmallWindow") as Boolean
            }
        }

        // StartSmallWindow
        loadClass("com.miui.home.smallwindow.SmallWindowStateHelperUseManager").methodFinder()
            .filterByName("canEnterMiniSmallWindow").first().createHook {
                before {
                    it.result = ObjectUtils.getObjectOrNullAs<ArraySet<*>>(
                        it.thisObject,
                        "mMiniSmallWindowInfoSet"
                    )!!.isEmpty()
                }
            }
        loadClass("miui.app.MiuiFreeFormManager").methodFinder()
            .filterByName("getAllFreeFormStackInfosOnDisplay")
            .toList().createHooks {
                before { param ->
                    if (Throwable().stackTrace.any {
                            it.className == "android.util.MiuiMultiWindowUtils" && it.methodName == "startSmallFreeform"
                        }) {
                        param.result = null
                    }
                }
            }
        loadClass("android.util.MiuiMultiWindowUtils").methodFinder()
            .filterByName("hasSmallFreeform").toList().createHooks {
                before { param ->
                    if (Throwable().stackTrace.any {
                            it.className == "android.util.MiuiMultiWindowUtils" && it.methodName == "startSmallFreeform"
                        }) {
                        param.result = false
                    }
                }
            }
    }
}
