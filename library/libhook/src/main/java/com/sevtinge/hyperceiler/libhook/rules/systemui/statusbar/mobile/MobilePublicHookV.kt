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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import android.telephony.SubscriptionManager
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Dependency
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MiuiStub
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.card1
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.card2
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.hideRoaming
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.isEnableDouble
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.hookAllConstructors
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import java.util.function.Consumer

class MobilePublicHookV : BaseHook() {
    override fun init() {
        // findClass(
        //     "com.android.systemui.statusbar.connectivity.NetworkControllerImpl"
        // ).setStaticObjectField("DEBUG", true)

        miuiCellularIconVM.hookAllConstructors {
            after { param ->
                val cellularIcon = param.thisObject
                val mobileIconInteractor = param.args[2] ?: return@after
                val subId = mobileIconInteractor.getObjectFieldAs<Int>("subId")

                // 双排信号
                if (isEnableDouble && !isMoreAndroidVersion(36)) {
                    val isVisible = if (isMoreAndroidVersion(36) || isMoreHyperOSVersion(3f)) {
                        val pair = loadClass("kotlin.Pair")
                            .getConstructor(Object::class.java, Object::class.java)
                            .newInstance(false, false)
                        newReadonlyStateFlow(pair)
                    } else {
                        newReadonlyStateFlow(false)
                    }
                    cellularIcon.setObjectField("isVisible", isVisible)
                    if (!hideRoaming) {
                        cellularIcon.setObjectField("smallRoamVisible", newReadonlyStateFlow(false))
                    }

                    Dependency.miuiLegacyDependency
                        ?.getObjectField("mOperatorCustomizedPolicy")
                        ?.callMethod("get")
                        ?.getObjectField("mobileIcons")
                        ?.getObjectField("activeMobileDataSubscriptionId")
                        ?.let { activeSubId ->
                            MiuiStub.javaAdapter.alwaysCollectFlow(
                                activeSubId,
                                Consumer<Int> {
                                    setStateFlowValue(isVisible, subId == it)
                                }
                            )
                        }
                } else {
                    val getSlotIndex = SubscriptionManager.getSlotIndex(subId)
                    if ((card1 && getSlotIndex == 0) || (card2 && getSlotIndex == 1)) {
                        if (isMoreAndroidVersion(36)) {
                            val pair = loadClass("kotlin.Pair")
                                .getConstructor(Object::class.java, Object::class.java)
                                .newInstance(false, false)
                            cellularIcon.setObjectField("isVisible", newReadonlyStateFlow(pair))
                        } else {
                            cellularIcon.setObjectField("isVisible", newReadonlyStateFlow(false))
                        }
                    }
                }

                if (hideIndicator) {
                    cellularIcon.setObjectField("inOutVisible", newReadonlyStateFlow(false))
                }
                if (hideRoaming) {
                    // 新版 MiuiCellularIconVM 中 *RoamVisible 字段类型为
                    // FlowKt__ZipKt$combine$$inlined$combineUnsafe$FlowKt__ZipKt$1
                    // （combine 产生的匿名 Flow），直接 setObjectField 会抛
                    // IllegalArgumentException。先尝试旧版直接替换 StateFlow，
                    // 失败则改为劫持其内部 $transform$inlined$1 lambda 让合并结果恒为 false。
                    forceRoamHidden(cellularIcon, "smallRoamVisible")
                    forceRoamHidden(cellularIcon, "mobileRoamVisible")
                }
                // 隐藏 hd
                if (!isMoreSmallVersion(200, 2f)) {
                    updateIconState(param, "smallHdVisible", "system_ui_status_bar_icon_small_hd")
                    updateIconState(param, "volteVisibleCn", "system_ui_status_bar_icon_big_hd")
                    updateIconState(param, "volteVisibleGlobal", "system_ui_status_bar_icon_big_hd")
                }
            }
        }
    }

    private fun forceRoamHidden(cellularIcon: Any, fieldName: String) {
        if (runCatching {
                cellularIcon.setObjectField(fieldName, newReadonlyStateFlow(false))
            }.isSuccess
        ) {
            return
        }

        runCatching {
            val flow = cellularIcon.getObjectFieldAs<Any>(fieldName)
            val transform = createAlwaysFalseTransform(flow) ?: return
            flow.setObjectField($$"$transform$inlined$1", transform)
        }
    }

    private fun createAlwaysFalseTransform(flow: Any): Any? {
        val originalTransform = runCatching {
            flow.getObjectFieldAs<Any>($$"$transform$inlined$1")
        }.getOrNull() ?: return null

        val interfaces = originalTransform.javaClass.interfaces
            .filter { it.name.startsWith("kotlin.jvm.functions.Function") }
            .toTypedArray()
        if (interfaces.isEmpty()) return null

        val classLoader = originalTransform.javaClass.classLoader ?: javaClass.classLoader
        return java.lang.reflect.Proxy.newProxyInstance(classLoader, interfaces) { _, method, _ ->
            when (method.name) {
                "invoke" -> false
                "toString" -> "AlwaysFalseTransform"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        }
    }

    private fun updateIconState(param: HookParam, fieldName: String, key: String) {
        val opt = PrefsBridge.getStringAsInt(key, 0)
        if (opt != 0) {
            val value = when (opt) {
                1 -> true
                else -> false
            }
            param.thisObject.setObjectField(fieldName, newReadonlyStateFlow(value))
        }
    }
}
