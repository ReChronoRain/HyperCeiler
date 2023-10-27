package com.sevtinge.hyperceiler.module.hook.phone

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object ModemFeature : BaseHook() {
    override fun init() {
        runCatching {
            loadClass("com.android.phone.FiveGManagerBase").methodFinder().first {
                name == "getModemFeatureMode"
            }.createHook {
                after {
                    it.args[0] = -1
                    it.result = true
                }
            }
        }

        runCatching {
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "isModemFeatureSupported"
            }.createHook {
                after {
                    it.args[0] = -1
                }
            }
        }

        runCatching {
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "getModemFeatureFromDb"
            }.createHook {
                after {
                    it.args[0] = -1
                }
            }
        }
    }
}
