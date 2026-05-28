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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter

import android.content.res.Resources
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.hookAllConstructors
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Field
import java.lang.reflect.Method

object SidebarLineCustom : BaseHook() {
    private const val STOCK_DEFAULT = -1294740525
    private const val STOCK_DARK = -6842473
    private const val STOCK_LIGHT = -872415232
    private const val STOCK_DARK_LEGACY = -855638017
    private const val PACKAGE_SECURITY_CENTER = "com.miui.securitycenter"

    private var updateLineColorMethod: Method? = null
    private var currentLineColorField: Field? = null
    private var sidebarLineColorDefault = STOCK_DEFAULT
    private var sidebarLineColorDark = STOCK_DARK
    private var sidebarLineColorLight = STOCK_LIGHT

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        updateLineColorMethod = optionalMember("SidebarLineUpdateColor") {
            it.findMethod {
                matcher {
                    addUsingString("updateLineColor: from", StringMatchType.Contains)
                }
            }.singleOrNull()
        } as? Method

        val updateMethod = updateLineColorMethod
        if (updateMethod == null) {
            XposedLog.w(TAG, packageName, "Sidebar line update color method not found")
            return false
        }

        currentLineColorField = optionalMember("SidebarLineCurrentColor") {
            it.findField {
                matcher {
                    declaredClass = updateMethod.declaringClass.name
                    type = "int"
                    addReadMethod {
                        declaredClass = updateMethod.declaringClass.name
                        name = updateMethod.name
                    }
                }
            }.singleOrNull()
        } as? Field

        return true
    }

    override fun init() {
        sidebarLineColorDefault =
            PrefsBridge.getInt("security_center_sidebar_line_color_default", STOCK_DEFAULT)
        sidebarLineColorDark =
            PrefsBridge.getInt("security_center_sidebar_line_color_dark", STOCK_DARK)
        sidebarLineColorLight =
            PrefsBridge.getInt("security_center_sidebar_line_color_light", STOCK_LIGHT)

        // debug
        XposedLog.d(TAG, PACKAGE_SECURITY_CENTER, "sidebarLineColorDefault is $sidebarLineColorDefault")
        XposedLog.d(TAG, PACKAGE_SECURITY_CENTER, "sidebarLineColorDark is $sidebarLineColorDark")
        XposedLog.d(TAG, PACKAGE_SECURITY_CENTER, "sidebarLineColorLight is $sidebarLineColorLight")

        setResReplacement(
            PACKAGE_SECURITY_CENTER,
            "color",
            "sidebar_line_color",
            sidebarLineColorDefault
        )
        setResReplacement(
            PACKAGE_SECURITY_CENTER,
            "color",
            "sidebar_line_color_dark",
            sidebarLineColorDark
        )
        setResReplacement(
            PACKAGE_SECURITY_CENTER,
            "color",
            "sidebar_line_color_light",
            sidebarLineColorLight
        )

        runCatching {
            Resources::class.java.findAllMethods {
                name("getColor")
            }.createBeforeHooks { param ->
                val resources = param.thisObject as? Resources ?: return@createBeforeHooks
                val resId = param.args.firstOrNull() as? Int ?: return@createBeforeHooks
                param.result = resources.getSidebarLineColor(resId) ?: return@createBeforeHooks
            }
        }.onFailure {
            XposedLog.w(TAG, PACKAGE_SECURITY_CENTER, "hook Resources.getColor failed", it)
        }

        val updateMethod = updateLineColorMethod ?: return
        val sidebarLineDrawableClass = updateMethod.declaringClass

        sidebarLineDrawableClass.hookAllConstructors {
            after { param ->
                applyInitialLineColor(param.thisObject)
            }
        }

        updateMethod.createBeforeHook { param ->
            val color = param.args.firstOrNull() as? Int ?: return@createBeforeHook
            param.args[0] = replaceStockColor(color)
        }
    }

    private fun applyInitialLineColor(drawable: Any) {
        val updateMethod = updateLineColorMethod ?: return
        val colorField = currentLineColorField ?: return
        runCatching {
            val currentColor = colorField.getInt(drawable)
            val targetColor = replaceStockColor(currentColor)
            if (targetColor != currentColor) {
                updateMethod.invoke(drawable, targetColor)
            }
        }.onFailure {
            XposedLog.w(TAG, PACKAGE_SECURITY_CENTER, "set sidebar line color failed", it)
        }
    }

    private fun replaceStockColor(color: Int): Int {
        return when (color) {
            STOCK_DEFAULT -> sidebarLineColorDefault
            STOCK_DARK, STOCK_DARK_LEGACY -> sidebarLineColorDark
            STOCK_LIGHT -> sidebarLineColorLight
            else -> color
        }
    }

    private fun Resources.getSidebarLineColor(resId: Int): Int? {
        return runCatching {
            when (getResourceEntryName(resId)) {
                "sidebar_line_color" -> sidebarLineColorDefault
                "sidebar_line_color_dark" -> sidebarLineColorDark
                "sidebar_line_color_light" -> sidebarLineColorLight
                else -> null
            }
        }.getOrNull()
    }
}
