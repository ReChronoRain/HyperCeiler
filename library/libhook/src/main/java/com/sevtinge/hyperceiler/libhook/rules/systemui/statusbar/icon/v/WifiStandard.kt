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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.v

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MiuiStub
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getStaticObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object WifiStandard : BaseHook() {
    override fun useDexKit() = !isMoreHyperOSVersion(4f)

    override fun initDexKit(): Boolean {
        if (!isMoreHyperOSVersion(4f)) makeWifiStandardZero
        return true
    }
    private val showWifi by lazy {
        PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0)
    }

    private val makeWifiStandardZero by lazy {
        optionalMember("makeWifiStandardZero") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass {
                        if (isMoreAndroidVersion(36)) {
                            className("WifiViewModelInject\$special\$", StringMatchType.Contains)
                        } else {
                            className("viewmodel.WifiViewModel\$special", StringMatchType.Contains)
                        }
                    }
                    usingNumbers(5, 0)
                    addInvoke("Ljava/lang/Integer;-><init>(I)V")

                    if (isMoreAndroidVersion(36)) {
                        addUsingField {
                            name("\$this_unsafeFlow")
                        }
                    }
                }
            }.singleOrNull()
        } as? Method
    }

    override fun init() {
        if (isMoreHyperOSVersion(4f)) {
            hookOS4()
            return
        }
        try {
            Constructors.find(loadClass("com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel"))
                .first()
                .createAfterHook {
                    if (showWifi == 1) {
                        if (isDebug()) {
                            try {
                                val wifiStandard =
                                    it.thisObject.getObjectFieldAs<Any>("wifiStandard")
                                MiuiStub.javaAdapter.alwaysCollectFlow<Int>(wifiStandard) { i ->
                                    XposedLog.d(TAG, lpparam.packageName, "wifiStandard $i")
                                }
                            } catch (e: Throwable) {
                                XposedLog.e(
                                    TAG,
                                    lpparam.packageName,
                                    "Failed to hook wifiStandard field",
                                    e
                                )
                            }
                        }
                    } else if (showWifi == 2) {
                        try {
                            it.thisObject.setObjectField("wifiStandard", newReadonlyStateFlow(0))
                        } catch (e: Throwable) {
                            XposedLog.e(
                                TAG,
                                lpparam.packageName,
                                "Failed to set wifiStandard field",
                                e
                            )
                        }
                    }
                }
        } catch (e: Throwable) {
            XposedLog.e(TAG, lpparam.packageName, "Failed to hook WifiViewModel constructor", e)
        }

        if (showWifi == 1) {
            val coroutineSingletons = loadClass("kotlin.coroutines.intrinsics.CoroutineSingletons")
            val suspended = coroutineSingletons.getStaticObjectField("COROUTINE_SUSPENDED")
            val unit = loadClass("kotlin.Unit").getStaticObjectField("INSTANCE")

            makeWifiStandardZero?.createBeforeHook {
                val wifiStandard = it.args[0]
                val continuation = it.args[1]
                if (wifiStandard != 0 && continuation?.getObjectField("label") != 0) {
                    val flow = it.thisObject.getObjectFieldAs<Any>("\$this_unsafeFlow")
                    val obj = flow.callMethod("emit", wifiStandard, continuation)
                    it.result = if (obj == suspended) {
                        suspended
                    } else {
                        unit
                    }
                }
            }
        }
    }

    private fun hookOS4() {
        val mode = PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0)
        if (mode !in 1..2) return
        val owner = "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModelInject"
        val producer = requiredClass(owner + "\$wifiStandardFlow\$1")
        if (mode == 2) {
            // This is the common source for both the number and wifiInoutLeft. Replacing
            // only WifiViewModel.wifiStandard leaves the latter on the old StateFlow.
            val method = exactMethod(producer, "invokeSuspend", Any::class.java)
            checkNotNull(hookMethod(method, object : IMethodHook {
                override fun after(param: HookParam) {
                    if (param.thisObject?.javaClass == producer && !param.hasThrowable && param.result is Int) param.result = 0
                }
            }))
            return
        }

        val outer = requiredClass(owner + "\$special\$\$inlined\$map\$1")
        val combine = requiredClass("kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$\$inlined\$combineUnsafe\$FlowKt__ZipKt\$1")
        val flow = requiredClass("kotlinx.coroutines.flow.Flow")
        val collector = requiredClass("kotlinx.coroutines.flow.FlowCollector")
        val continuation = requiredClass("kotlin.coroutines.Continuation")
        val function = requiredClass("kotlin.Function")
        check(flow.isAssignableFrom(outer) && flow.isAssignableFrom(combine))
        val outerId = exactField(outer, "\$r8\$classId", Int::class.java)
        val source = exactField(outer, "\$this_unsafeTransform\$inlined", Any::class.java)
        val combineId = exactField(combine, "\$r8\$classId", Int::class.java)
        val transform = exactField(combine, "\$transform\$inlined\$1", function)
        val inputs = exactField(combine, "\$flows\$inlined", java.lang.reflect.Array.newInstance(flow, 0).javaClass)
        val method = exactMethod(outer, "collect", collector, continuation)
        val collect = exactMethod(combine, "collect", collector, continuation)
        val suspended = requiredClass("kotlin.coroutines.intrinsics.CoroutineSingletons")
            .getField("COROUTINE_SUSPENDED").get(null)
        val unit = requiredClass("kotlin.Unit").getField("INSTANCE").get(null)
        checkNotNull(hookMethod(method, object : IMethodHook {
            // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
            @Suppress("TooGenericExceptionCaught")
            override fun before(param: HookParam) {
                if (param.isSkipped) return
                val upstream: Any
                try {
                    // R8 shares this method with metered/default-network/combine flows.
                    // The standard map is OUTER id3; its INNER collector is id2.
                    if (param.thisObject?.javaClass != outer || outerId.getInt(param.thisObject) != 3) return
                    upstream = source.get(param.thisObject) ?: return
                    if (upstream.javaClass != combine || combineId.getInt(upstream) != 2) return
                    if (transform.get(upstream)?.javaClass != producer) return
                    val flows = inputs.get(upstream) as? Array<*> ?: return
                    if (flows.size != 5 || flows.any { !flow.isInstance(it) }) return
                    if (!collector.isInstance(param.args[0]) || !continuation.isInstance(param.args[1])) return
                } catch (error: ReflectiveOperationException) {
                    debugCallbackError("OS4 WiFi standard shape", error)
                    return
                } catch (error: RuntimeException) {
                    debugCallbackError("OS4 WiFi standard shape", error)
                    return
                }
                try {
                    // Preserve the real upstream state machine and the caller continuation.
                    // Only bypass the map which changes standards <=5 into zero.
                    val result = collect.invoke(upstream, param.args[0], param.args[1])
                    param.result = if (result === suspended) suspended else unit
                } catch (error: InvocationTargetException) {
                    // Once collection starts, never fall back and collect a second time.
                    param.throwable = error.targetException
                } catch (error: ReflectiveOperationException) {
                    debugCallbackError("OS4 WiFi standard collect access", error)
                }
            }
        }))
    }

    private fun requiredClass(name: String): Class<*> =
        checkNotNull(findClassIfExists(name)) { "Missing OS4 WiFi class: $name" }

    private fun exactMethod(owner: Class<*>, name: String, vararg parameters: Class<*>): Method =
        owner.getDeclaredMethod(name, *parameters).apply {
            check(!Modifier.isStatic(modifiers) && returnType == Any::class.java)
            isAccessible = true
        }

    private fun exactField(owner: Class<*>, name: String, type: Class<*>): Field =
        owner.getDeclaredField(name).apply {
            check(!Modifier.isStatic(modifiers) && this.type == type)
            isAccessible = true
        }

}
