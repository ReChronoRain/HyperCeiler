package com.sevtinge.hyperceiler.module.hook.phone

import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object ViceSlotVolteButton : BaseHook() {
    override fun init() {
        runCatching {
            // exec("settings put global vice_slot_volte_data_enabled 1")
            Settings.Global.putInt(
                findContext(FlAG_ONLY_ANDROID).contentResolver,
                "vice_slot_volte_data_enabled",
                1
            )
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
