package com.sevtinge.cemiuiler.module.phone

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.exec

object ViceSlotVolteButton : BaseHook() {
    override fun init() {
        runCatching {
            exec("settings put global vice_slot_volte_data_enabled 1")
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "shouldHideViceSlotVolteDataButton"
            }.createHook {
                returnConstant(false)
            }
        }
        runCatching {
            loadClass("com.android.phone.MiuiPhoneUtils").methodFinder().first {
                name == "shouldHideSmartDualSimButton"
            }.createHook {
                returnConstant(false)
            }
        }
    }
}
