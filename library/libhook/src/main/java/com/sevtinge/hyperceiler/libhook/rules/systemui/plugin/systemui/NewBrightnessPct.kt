/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.view.View
import android.view.ViewGroup
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object NewBrightnessPct {
    fun initLoaderHook(classLoader: ClassLoader) {
        if (isMoreHyperOSVersion(3f)) {
            loadClass($$"miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelSliderDelegate$prepareShow$5", classLoader)
                .methodFinder().filterByName("onStartTrackingTouch")
                .first().createBeforeHook {
                    startPct(it)
                }

            loadClass($$"miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelSliderDelegate$seekBarListener$1", classLoader)
                .methodFinder().filterByName("onStartTrackingTouch")
                .first().createBeforeHook {
                    startPct(it)
                }

        } else {
            loadClass($$"miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController$seekBarListener$1", classLoader)
                .methodFinder().filterByName("onStartTrackingTouch")
                .first().createBeforeHook {
                    startPct(it)
                }

            loadClass($$"miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController$seekBarListener$1", classLoader)
                .methodFinder().filterByName("onStartTrackingTouch")
                .first().createBeforeHook {
                    startPct(it)
                }
        }
    }

    private fun startPct(it: BeforeHookParam) {
        val windowView = getView("miui.systemui.dagger.PluginComponentFactory", it.thisObject.javaClass.classLoader)
        if (windowView == null) {
            XposedLog.e("NewBrightnessPct", "ControlCenterWindowViewImpl is null")
            return
        }
        AppsTool.initPct(windowView as ViewGroup, 2)
        AppsTool.mPct.visibility = View.VISIBLE
    }

    private fun getView(str: String, cl: ClassLoader?): Any? {
        val cl2 = loadClass(str, cl)
        val controlCenterWindowView = cl2.callStaticMethod("getInstance")!!
            .callMethod("getPluginComponent")!!
            .getObjectField("controlCenterWindowViewCreatorProvider")!!
            .callMethod("get")!!
            .getObjectField("windowView")
        return controlCenterWindowView
    }
}
