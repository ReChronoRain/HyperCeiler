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
package com.sevtinge.hyperceiler.libhook.utils.hookapi

import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getFirstFieldByExactType

object StateFlowHelper {
     private val STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.StateFlow")
    }

    private val STATE_FLOW_KT by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.StateFlowKt")
    }

    private val READONLY_STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.ReadonlyStateFlow")
    }

    private val MUTABLE_STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.MutableStateFlow")
    }

    private val READONLY_STATE_FLOW_CONSTRUCTOR by lazy {
        if (isMoreAndroidVersion(36)) {
            READONLY_STATE_FLOW.getConstructor(MUTABLE_STATE_FLOW)
        } else {
            READONLY_STATE_FLOW.getConstructor(STATE_FLOW)
        }
    }

    @JvmStatic
    fun newStateFlow(initValue: Any?): Any {
        return STATE_FLOW_KT.callStaticMethod("MutableStateFlow", initValue) as Any
    }

    @JvmStatic
    fun newReadonlyStateFlow(initValue: Any?): Any {
        return READONLY_STATE_FLOW_CONSTRUCTOR.newInstance(newStateFlow(initValue))
    }

    @JvmStatic
    fun setStateFlowValue(stateFlow: Any?, value: Any?) {
        stateFlow ?: return

        when (stateFlow::class.java.simpleName) {
            "ReadonlyStateFlow" -> {
                if (isMoreAndroidVersion(36)) {
                    stateFlow.getFirstFieldByExactType(MUTABLE_STATE_FLOW)
                } else {
                    stateFlow.getFirstFieldByExactType(STATE_FLOW)
                }
            }
            "StateFlowImpl" -> stateFlow
            else -> null
        }?.callMethod("setValue", value)
    }

    @JvmStatic
    fun getStateFlowValue(stateFlow: Any?): Any? {
        return stateFlow?.callMethod("getValue")
    }
}
