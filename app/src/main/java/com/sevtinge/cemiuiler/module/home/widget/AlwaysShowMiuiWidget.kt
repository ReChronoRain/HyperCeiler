package com.sevtinge.cemiuiler.module.home.widget

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook

object AlwaysShowMiuiWidget : BaseHook() {
    override fun init() {
        var hook1: XC_MethodHook.Unhook? = null
        var hook2: XC_MethodHook.Unhook? = null
        try {
            loadClass("com.miui.home.launcher.widget.WidgetsVerticalAdapter").methodFinder().first {
                name == "buildAppWidgetsItems"
            }
        } catch (e: Exception) {
            loadClass("com.miui.home.launcher.widget.BaseWidgetsVerticalAdapter").methodFinder().first {
                name == "buildAppWidgetsItems"
            }
        }.createHook {
            before {
                hook1 = loadClass("com.miui.home.launcher.widget.MIUIAppWidgetInfo").methodFinder()
                    .first {
                        name == "initMiuiAttribute" && parameterCount == 1
                    }.createHook {
                        after {
                            it.thisObject.setObjectField("isMIUIWidget", false)
                        }
                    }
                hook2 = loadClass("com.miui.home.launcher.MIUIWidgetUtil").methodFinder().first {
                    name == "isMIUIWidgetSupport"
                }.createHook {
                    after {
                        it.result = false
                    }
                }
            }
            after {
                hook1?.unhook()
                hook2?.unhook()
            }
        }
    }
}
