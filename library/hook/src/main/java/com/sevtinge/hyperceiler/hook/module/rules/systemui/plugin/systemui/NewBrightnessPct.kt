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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui

import android.view.View
import android.view.ViewGroup
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils
import de.robv.android.xposed.XC_MethodHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
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

    private fun startPct(it: XC_MethodHook.MethodHookParam) {
        val windowView = getView("miui.systemui.dagger.PluginComponentFactory", it.thisObject.javaClass.classLoader)
        if (windowView == null) {
            XposedLogUtils.logE("NewBrightnessPct", "ControlCenterWindowViewImpl is null")
            return
        }
        OtherTool.initPct(windowView as ViewGroup, 2)
        OtherTool.mPct.visibility = View.VISIBLE
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
