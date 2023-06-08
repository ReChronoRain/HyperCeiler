package com.sevtinge.cemiuiler.module.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook

class MaxFreeForm : BaseHook() {
    override fun init() {
        // CanTaskEnterMiniSmallWindow
        loadClass("com.miui.home.launcher.RecentsAndFSGestureUtils").methodFinder().filter {
            name == "canTaskEnterMiniSmallWindow"
        }.toList().createHooks {
            returnConstant(true)
        }

        // CanTaskEnterSmallWindow
        loadClass("com.miui.home.launcher.RecentsAndFSGestureUtils").methodFinder().filter {
            name == "canTaskEnterSmallWindow"
        }.toList().createHooks {
            returnConstant(true)
        }

        // StartSmallWindow
        var hook1: List<XC_MethodHook.Unhook>?
        var hook2: List<XC_MethodHook.Unhook>? = null

        loadClass("com.miui.home.recents.views.RecentsTopWindowCrop").methodFinder().filter {
            name == "startSmallWindow"
        }.toList().createHooks {
            before {
                hook1 = loadClass("android.util.MiuiMultiWindowUtils").methodFinder().filter {
                    name == "startSmallFreeform" && paramCount == 4
                }.toList().createHooks {
                    before {
                        it.args[3] = false
                        hook2 = loadClass("miui.app.MiuiFreeFormManager").methodFinder().filter {
                            name == "getAllFreeFormStackInfosOnDisplay"
                        }.toList().createHooks {
                            before { param ->
                                param.result = null
                            }
                        }
                    }
                }
                loadClass("android.util.MiuiMultiWindowUtils").methodFinder().filter {
                    name == "startSmallFreeform"
                }.toList().createHooks {
                    after {
                        hook2?.forEach {
                            it.unhook()
                        }
                    }
                }
                loadClass("com.miui.home.recents.views.RecentsTopWindowCrop").methodFinder()
                    .filter {
                        name == "startSmallWindow"
                    }.toList().createHooks {
                        after {
                            hook1?.forEach {
                                it.unhook()
                            }
                        }
                    }
            }
        }
    }
}