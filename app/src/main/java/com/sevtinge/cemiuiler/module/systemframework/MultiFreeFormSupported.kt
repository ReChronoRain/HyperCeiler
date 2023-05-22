package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object MultiFreeFormSupported : BaseHook() {
    override fun init() {
        try {
            if (!mPrefsMap.getBoolean("system_framework_freeform_recents_to_small_freeform")) {
                findMethod("android.util.MiuiMultiWindowUtils") {
                    name == "multiFreeFormSupported"
                }.hookBefore {
                    val ex = Throwable()
                    val stackTrace = ex.stackTrace
                    var mResult = true
                    for (i in stackTrace) {
                        if (i.className == "com.android.server.wm.MiuiFreeFormGestureController\$FreeFormReceiver") {
                            mResult = false
                            break
                        }
                    }
                    it.result = mResult
                }
                log("Hook with recents_to_small_freeform success!")
            } else {
                findMethod("android.util.MiuiMultiWindowUtils") {
                    name == "multiFreeFormSupported"
                }.hookReturnConstant(true)
                log("Hook success!")
            }
        } catch (e: Throwable) {
            log("Hook failed by $e")
        }
    }

}