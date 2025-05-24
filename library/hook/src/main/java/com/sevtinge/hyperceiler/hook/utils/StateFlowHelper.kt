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
package com.sevtinge.hyperceiler.hook.utils

import com.github.kyuubiran.ezxhelper.EzXHelper
import de.robv.android.xposed.XposedHelpers

object StateFlowHelper {
     val STATE_FLOW by lazy {
        XposedHelpers.findClass("kotlinx.coroutines.flow.StateFlow", EzXHelper.classLoader)
    }

    private val STATE_FLOW_KT by lazy {
        XposedHelpers.findClass("kotlinx.coroutines.flow.StateFlowKt", EzXHelper.classLoader)
    }

    private val READONLY_STATE_FLOW by lazy {
        XposedHelpers.findClass("kotlinx.coroutines.flow.ReadonlyStateFlow", EzXHelper.classLoader)
    }

    private val READONLY_STATE_FLOW_CONSTRUCTOR by lazy {
        READONLY_STATE_FLOW.getConstructor(STATE_FLOW)
    }

    @JvmStatic
    fun newStateFlow(initValue: Any?): Any {
        return STATE_FLOW_KT.callStaticMethodAs("MutableStateFlow", initValue)
    }

    @JvmStatic
    fun newReadonlyStateFlow(initValue: Any?): Any {
        return READONLY_STATE_FLOW_CONSTRUCTOR.newInstance(newStateFlow(initValue))
    }

    @JvmStatic
    fun setStateFlowValue(stateFlow: Any?, value: Any?) {
        stateFlow ?: return

        when (stateFlow::class.java.simpleName) {
            "ReadonlyStateFlow" -> stateFlow.getFirstFieldByExactType(STATE_FLOW)
            "StateFlowImpl" -> stateFlow
            else -> null
        }?.callMethod("setValue", value)
    }

    @JvmStatic
    fun getStateFlowValue(stateFlow: Any?): Any? {
        return stateFlow?.callMethod("getValue")
    }
}
