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
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object NewBrightnessPct {
    fun initLoaderHook(classLoader: ClassLoader) {
        if (isMoreHyperOSVersion(3f)) {
            loadClass($$"miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelSliderDelegate$prepareShow$5", classLoader)
                .findMethod { name("onStartTrackingTouch") }
                .createBeforeHook {
                    startPct(it)
                }

            loadClass($$"miui.systemui.controlcenter.panel.secondary.brightness.BrightnessPanelSliderDelegate$seekBarListener$1", classLoader)
                .findMethod { name("onStartTrackingTouch") }
                .createBeforeHook {
                    startPct(it)
                }

        } else {
            loadClass($$"miui.systemui.controlcenter.panel.main.brightness.BrightnessSliderController$seekBarListener$1", classLoader)
                .findMethod { name("onStartTrackingTouch") }
                .createBeforeHook {
                    startPct(it)
                }

            loadClass($$"miui.systemui.controlcenter.panel.main.brightness.BrightnessPanelSliderController$seekBarListener$1", classLoader)
                .findMethod { name("onStartTrackingTouch") }
                .createBeforeHook {
                    startPct(it)
                }
        }
    }

    private fun startPct(it: HookParam) {
        val windowView = getView("miui.systemui.dagger.PluginComponentFactory", it.thisObject.javaClass.classLoader)
        if (windowView == null) {
            XposedLog.e("NewBrightnessPct", "ControlCenterWindowViewImpl is null")
            return
        }
        AppsTool.initPct(windowView as ViewGroup, 2)
        AppsTool.mPct.visibility = View.VISIBLE
    }

    private fun getView(str: String, cl: ClassLoader?): Any? {
        val loader = cl ?: return null
        val cl2 = loadClass(str, loader)
        val controlCenterWindowView = cl2.callStaticMethod("getInstance")!!
            .callMethod("getPluginComponent")!!
            .getObjectField("controlCenterWindowViewCreatorProvider")!!
            .callMethod("get")!!
            .getObjectField("windowView")
        return controlCenterWindowView
    }
}
