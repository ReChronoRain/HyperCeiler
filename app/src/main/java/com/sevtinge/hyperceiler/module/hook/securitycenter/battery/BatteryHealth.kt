package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import android.os.Bundle
import android.os.Message
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import java.io.File


object BatteryHealth : BaseHook() {
    private const val battery = "/sys/class/power_supply/battery/"
    private const val full = "charge_full"
    private const val design = "charge_full_design"


    override fun init() {
        findClassIfExists("com.miui.powercenter.nightcharge.SmartChargeFragment").let { c ->
            c.hookAfterMethod("onCreatePreferences", Bundle::class.java, String::class.java) {
                it.thisObject.getObjectField("c")?.callMethod("setText", "asd")
            }
        }

        findClassIfExists("com.miui.powercenter.nightcharge.SmartChargeFragment\$c").let { c ->
            c.hookAfterMethod("handleMessage", Message::class.java) {
                // TODO hardcode。想办法改进
                it.thisObject.getObjectField("a")!!.callMethod("get")!!
                    .getObjectField("c")!!.callMethod("setText", health())
            }
        }
    }


    private fun health(): String {
        val f = File(battery + full).readText()
        val d = File(battery + design).readText()
        return sswr(f.toFloat() / d.toFloat() * 100) + " %"
    }


    private fun sswr(x: Float): String {
        val s = x.toString()
        // 无需处理
        if (s.length <= 5) return s

        return if (s[5].digitToInt() >= 5)
            (x + 0.01).toString().substring(0, 5)
        else s.substring(0, 5)
    }
}
