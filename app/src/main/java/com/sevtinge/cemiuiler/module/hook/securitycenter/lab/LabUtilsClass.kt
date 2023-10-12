package com.sevtinge.cemiuiler.module.hook.securitycenter.lab

import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object LabUtilsClass {
    val labUtilClass by lazy {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("mi_lab_ai_clipboard_enable", "mi_lab_blur_location_enable")
            }
        }
    }
}
