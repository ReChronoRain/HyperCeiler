package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.module.hook.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.getStaticObjectField
import com.sevtinge.hyperceiler.utils.setObjectField
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object WifiStandard : BaseHook() {
    private val showWifi by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0)
    }

    private val makeWifiStandardZero by lazy {
        DexKit.findMember("makeWifiStandardZero") { bridge ->
            bridge.findClass {
                matcher {
                    className("viewmodel.WifiViewModel\$special", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
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
