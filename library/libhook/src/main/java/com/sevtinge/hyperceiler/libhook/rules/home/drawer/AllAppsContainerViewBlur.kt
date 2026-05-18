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
package com.sevtinge.hyperceiler.libhook.rules.home.drawer

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.ViewSwitcher
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getCornerRadiusTop
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.zhenxiang.BlurFrameLayout
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.zhenxiang.model.CornersRadius
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField


object AllAppsContainerViewBlur : BaseHook() {
    override fun init() {
        Application::class.java.findMethod {
            name("attach")
        }.createBeforeHook {
            EzXposed.initAppContext(it.args[0] as Context)

            loadClass("com.miui.home.launcher.allapps.BaseAllAppsContainerView").findAllMethods {
                filter { name == "onFinishInflate" }
            }.createHooks {
                after { hookParam ->
                    val mCategoryContainer = hookParam.thisObject.getObjectField("mCategoryContainer") as ViewSwitcher
                    val appsView = mCategoryContainer.parent as RelativeLayout
                    val blur = BlurFrameLayout(mCategoryContainer.context)
                    val radius = getCornerRadiusTop().toFloat()
                    blur.blurController.apply {
                        cornerRadius = CornersRadius.custom(radius, radius, 0f, 0f)
                    }
                    val view = View(mCategoryContainer.context)
                    blur.addView(view)
                    (view.layoutParams as FrameLayout.LayoutParams).apply {
                        width = FrameLayout.LayoutParams.MATCH_PARENT
                        height = FrameLayout.LayoutParams.MATCH_PARENT
                    }
                    appsView.addView(blur, 0)

                    loadClass("com.miui.home.launcher.allapps.BaseAllAppsContainerView").findAllMethods {
                        filter { name == "onResume" }
                    }.createHooks {
                        after {
                            blur.refreshDrawableState()
                        }
                    }
                }
            }
        }
    }
}
