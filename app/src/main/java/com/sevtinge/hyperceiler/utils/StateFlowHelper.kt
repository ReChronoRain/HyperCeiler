package com.sevtinge.hyperceiler.utils

import com.github.kyuubiran.ezxhelper.*
import de.robv.android.xposed.*

object StateFlowHelper {
    private const val STATE_FLOW = "kotlinx.coroutines.flow.StateFlow"
    private const val STATE_FLOW_KT = "kotlinx.coroutines.flow.StateFlowKt"
    private const val READONLY_STATE_FLOW = "kotlinx.coroutines.flow.ReadonlyStateFlow"

    private val stateFlowClz
        get() = XposedHelpers.findClass(STATE_FLOW, EzXHelper.classLoader)

    private val stateFlowKtClz
        get() = XposedHelpers.findClass(STATE_FLOW_KT, EzXHelper.classLoader)

    private val readonlyStateFlowClz
        get() = XposedHelpers.findClass(READONLY_STATE_FLOW, EzXHelper.classLoader)

    private val readonlyStateFlowConstructor
        get() = readonlyStateFlowClz.getConstructor(stateFlowClz)

    @JvmStatic
    fun newStateFlow(initValue : Any?): Any {
        return stateFlowKtClz.callStaticMethodAs("MutableStateFlow", initValue)
    }

    @JvmStatic
    fun newReadonlyStateFlow(initValue : Any?): Any {
        return readonlyStateFlowConstructor.newInstance(newStateFlow(initValue))
    }

    @JvmStatic
    fun setStateFlowValue(stateFlow : Any?, value: Any?) {
        stateFlow ?: return

        when (stateFlow::class.java.simpleName) {
            "ReadonlyStateFlow" -> stateFlow.getFirstFieldByExactType(stateFlowClz)
            "StateFlowImpl" -> stateFlow
            else -> null
        }?.callMethod("setValue", value)
    }

    @JvmStatic
    fun getStateFlowValue(stateFlow : Any?): Any? {
        return stateFlow?.callMethod("getValue")
    }
}