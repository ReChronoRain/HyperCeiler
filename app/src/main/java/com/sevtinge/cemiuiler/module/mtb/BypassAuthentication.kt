package com.sevtinge.cemiuiler.module.mtb

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.setObjectField

object BypassAuthentication : BaseHook() {
    override fun init() {
        val mModemTestBoxClass = loadClass("com.xiaomi.mtb.activity.ModemTestBoxMainActivity")

        try {
            loadClass("com.xiaomi.mtb.MtbApp").methodFinder().first {
                name == "setMiServerPermissionClass"
            }.createHook {
                before {
                    it.args[0] = 0
                }
            }
        } catch (_: Throwable) {
        }

        try {
            mModemTestBoxClass.methodFinder().first {
                name == "updateClass"
            }.createHook {
                before {
                    it.args[0] = 0
                    it.thisObject.setObjectField("mClassNet", 0)
                }
            }
        } catch (_: Throwable) {
        }

        try {
            mModemTestBoxClass.methodFinder().first {
                name == "initClassProduct"
            }.createHook {
                after {
                    it.thisObject.setObjectField("mClassProduct", 0)
                }
            }
        } catch (_: Throwable) {
        }
    }

}
