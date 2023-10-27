package com.sevtinge.hyperceiler.module.hook.nfc

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

class AllowInformationScreen : BaseHook() {
    override fun init() {
        loadClass("com.android.nfc.NfcService").methodFinder().filterByName("sendMessage").first().createHook {
            before {
                if (it.args[1] == 2 || it.args[1] == 4) {
                    it.args[1] = 8
                }
            }
        }
    }
}
