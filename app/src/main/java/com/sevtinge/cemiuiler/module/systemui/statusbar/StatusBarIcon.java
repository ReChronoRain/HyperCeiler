package com.sevtinge.cemiuiler.module.systemui.statusbar;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class StatusBarIcon extends BaseHook {

    @Override
    public void init() {

        MethodHook mIconHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String iconType = (String) param.args[0];
                switch (checkSlot(iconType)) {
                    case 1 -> param.args[1] = true;
                    case 2 -> param.args[1] = false;
                }
            }
        };

        findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl", "setIconVisibility", String.class, boolean.class, mIconHook);
        findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", "setIconVisibility", String.class, boolean.class, mIconHook);
    }


    private static int checkSlot(String slotName) {
        int vpn = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_vpn", 0);
        int alarmClock = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0);
        int nfc = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_nfc", 0);
        int zen = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_zen", 0);
        int volume = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_volume", 0);
        int wifi = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0);
        int wifi_slave = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_slave", 0);
        int airplane = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_airplane", 0);
        int location = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_location", 0);
        int hotspot = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_hotspot", 0);
        int headset = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_headset", 0);

        return switch (slotName) {
            case "vpn" ->// vpn
                isEnable(vpn) ? vpn : 0;
            case "alarm_clock" ->// 闹钟
                isEnable(alarmClock) ? alarmClock : 0;
            case "nfc" ->// nfc
                isEnable(nfc) ? nfc : 0;
            case "zen" ->// 勿扰模式
                isEnable(zen) ? zen : 0;
            case "volume" ->// 声音
                isEnable(volume) ? volume : 0;
            case "wifi" ->// wifi
                isEnable(wifi) ? wifi : 0;
            case "wifi_slave" ->// 辅助wifi
                isEnable(wifi_slave) ? wifi_slave : 0;
            case "airplane" ->// 飞行模式
                isEnable(airplane) ? airplane : 0;
            case "location" ->// 位置信息
                isEnable(location) ? location : 0;
            case "hotspot" ->// 热点
                isEnable(hotspot) ? hotspot : 0;
            case "headset" ->// 耳机
                isEnable(headset) ? headset : 0;
            default -> 0;
        };
    }

    private static boolean isEnable(int i) {
        return 0 < i && i < 3;
    }
}
