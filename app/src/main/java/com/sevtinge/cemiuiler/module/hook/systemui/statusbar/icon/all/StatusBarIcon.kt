package com.sevtinge.cemiuiler.module.hook.systemui.statusbar.icon.all

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.devicesdk.isAndroidU

class StatusBarIcon : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl").methodFinder().first {
            name == "setIconVisibility" && paramCount == 2
        }.createHook {
            before { param ->
                val iconType = param.args[0] as String
                when (checkSlot(iconType)) {
                    1 -> param.args[1] = true
                    2 -> param.args[1] = false
                }
            }
        }


        loadClass(if (isAndroidU())
            "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl"
        else
            "com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl").methodFinder().first {
            name == "setIconVisibility" && paramCount == 2
        }.createHook {
            before { param ->
                val iconType = param.args[0] as String
                when (checkSlot(iconType)) {
                    1 -> param.args[1] = true
                    2 -> param.args[1] = false
                }
            }
        }
    }

    companion object {
        private fun checkSlot(slotName: String): Int {
            val vpn = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_vpn", 0)
            val alarmClock = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0)
            val nfc = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_nfc", 0)
            val zen = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_zen", 0)
            val volume = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_volume", 0)
            val wifi = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0)
            val wifi_slave = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_slave", 0)
            val airplane = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_airplane", 0)
            val location = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_location", 0)
            val hotspot = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_hotspot", 0)
            val headset = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_headset", 0)
            return when (slotName) {
                "vpn" ->  // vpn
                    if (isEnable(vpn)) vpn else 0

                "alarm_clock" ->  // 闹钟
                    if (isEnable(alarmClock)) alarmClock else 0

                "nfc" ->  // nfc
                    if (isEnable(nfc)) nfc else 0

                "zen" ->  // 勿扰模式
                    if (isEnable(zen)) zen else 0

                "volume" ->  // 声音
                    if (isEnable(volume)) volume else 0

                "wifi" ->  // wifi
                    if (isEnable(wifi)) wifi else 0

                "wifi_slave" ->  // 辅助wifi
                    if (isEnable(wifi_slave)) wifi_slave else 0

                "airplane" ->  // 飞行模式
                    if (isEnable(airplane)) airplane else 0

                "location" ->  // 位置信息
                    if (isEnable(location)) location else 0

                "hotspot" ->  // 热点
                    if (isEnable(hotspot)) hotspot else 0

                "headset" ->  // 耳机
                    if (isEnable(headset)) headset else 0

                else -> 0
            }
        }

        private fun isEnable(i: Int): Boolean {
            return i in 1..2
        }
    }
}
