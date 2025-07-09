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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.hook.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getStaticObjectField
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object WifiStandard : BaseHook() {
    private val showWifi by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0)
    }

    private val makeWifiStandardZero by lazy {
        DexKit.findMember("makeWifiStandardZero") { bridge ->
            bridge.findMethod {
                matcher {
                    declaredClass {
                        className("viewmodel.WifiViewModel\$special", StringMatchType.Contains)
                    }
                    usingNumbers(5, 0)
                    addInvoke("Ljava/lang/Integer;-><init>(I)V")
                }
            }.singleOrNull()
        } as? Method
    }

    override fun init() {
        loadClass("com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel")
            .constructorFinder()
            .first()
            .createAfterHook {
                if (showWifi == 1) {
                    if (isDebug()) {
                        val wifiStandard = it.thisObject.getObjectFieldAs<Any>("wifiStandard")
                        MiuiStub.javaAdapter.alwaysCollectFlow<Int>(wifiStandard) { i ->
                            logD("wifiStandard $i")
                        }

                        // it.thisObject.setObjectField("inoutLeft", newReadonlyStateFlow(true))
                    }
                } else if (showWifi == 2) {
                    it.thisObject.setObjectField("wifiStandard", newReadonlyStateFlow(0))
                }
            }


        if (showWifi == 1) {
            val coroutineSingletons = loadClass("kotlin.coroutines.intrinsics.CoroutineSingletons")
            val suspended = coroutineSingletons.getStaticObjectField("COROUTINE_SUSPENDED")
            val unit = loadClass("kotlin.Unit").getStaticObjectField("INSTANCE")

            makeWifiStandardZero?.createBeforeHook {
                val wifiStandard = it.args[0]
                val continuation = it.args[1]
                if (wifiStandard != 0 && continuation.getObjectField("label") != 0) {
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
}
