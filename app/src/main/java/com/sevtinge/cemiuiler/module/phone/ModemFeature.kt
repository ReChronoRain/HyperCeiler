package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object ModemFeature : BaseHook() {
    override fun init() {
        try {
            loadClass("com.android.phone.FiveGManagerBase").methodFinder().first {
                name == "getModemFeatureMode"
            }.createHook {
                after {
                    it.args[0] = -1
                    it.result = true
                }
            }
        } catch (_: Throwable) {
        }

        try {
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "isModemFeatureSupported"
            }.createHook {
                after {
                    it.args[0] = -1
                }
            }
        } catch (_: Throwable) {
        }

        try {
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "getModemFeatureFromDb"
            }.createHook {
                after {
                    it.args[0] = -1
                }
            }
        } catch (_: Throwable) {
        }
    }
}