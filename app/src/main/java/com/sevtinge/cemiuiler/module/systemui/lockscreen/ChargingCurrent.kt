package com.sevtinge.cemiuiler.module.systemui.lockscreen

import com.sevtinge.cemiuiler.module.base.BaseHook
import android.app.AndroidAppHelper
import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.abs

object ChargingCurrent : BaseHook() {

    override fun init() {
        Application::class.java.hookBeforeMethod("attach", Context::class.java) { it ->
            EzXHelperInit.initHandleLoadPackage(lpparam)
            EzXHelperInit.setLogTag(TAG)
            EzXHelperInit.setToastTag(TAG)
            EzXHelperInit.initAppContext(it.args[0] as Context)

            findMethod("com.android.keyguard.charge.ChargeUtils") {
                name == "getChargingHintText" && parameterCount == 3
            }.hookAfter {
                it.result = "${getCurrent()}\n${it.result}"
            }

            findMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView") {
                name == "onFinishInflate"
            }.hookAfter {
                (it.thisObject.getObject("mIndicationText") as TextView).isSingleLine = false
            }
        }
    }



    private fun getCurrent(): String {
        val batteryManager = AndroidAppHelper.currentApplication().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current = abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
        return "${InitFields.moduleRes.getString(R.string.system_ui_lock_screen_current)} ${current}mA"
    }

    /**
     * 获取平均电流值
     * 获取 filePath 文件 totalCount 次数的平均值，每次采样间隔 intervalMs 时间
     */
    private fun getMeanCurrentVal(filePath: String, totalCount: Int, intervalMs: Int): Float {
        var meanVal = 0.0f
        if (totalCount <= 0) {
            return 0.0f
        }
        for (i in 0 until totalCount) {
            try {
                val f: Float = readFile(filePath, 0).toFloat()
                meanVal += f / totalCount
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (intervalMs <= 0) {
                continue
            }
            try {
                Thread.sleep(intervalMs.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return meanVal
    }

    private fun readFile(path: String, defaultValue: Int): Int {
        try {
            val bufferedReader = BufferedReader(FileReader(path))
            val i: Int = bufferedReader.readLine().toInt(10)
            bufferedReader.close()
            return i
        } catch (_: java.lang.Exception) {
        }
        return defaultValue
    }

}