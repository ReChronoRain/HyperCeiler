package com.sevtinge.cemiuiler.module.systemui.statusbar;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class StatusBarIcon extends BaseHook {

    @Override
    public void init() {

        MethodHook mIconHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String iconType = (String)param.args[0];
                switch (checkSlot(iconType)) {
                    case 1:
                        param.args[1] = true;
                        break;
                    case 2:
                        param.args[1] = false;
                        break;
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

        switch (slotName) {
            case "vpn":
                return isEnable(vpn) ? vpn : 0;
            case "alarm_clock":
                return isEnable(alarmClock) ? alarmClock : 0;
            case "nfc":
                return isEnable(nfc) ? nfc : 0;
            default:
                return 0;
        }
    }

    private static boolean isEnable(int i) {
        return 0 < i && i < 3;
    }
}
