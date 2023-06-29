package com.sevtinge.cemiuiler.module.systemui.statusbar.icon.all;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BluetoothIcon extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", "updateBluetooth", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0);
                int opt_b = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth_battery", 0);
                boolean isBluetoothConnected = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBluetooth"), "isBluetoothConnected");
                Object mIconController = XposedHelpers.getObjectField(param.thisObject, "mIconController");
                if (opt == 2 || (opt == 3 && !isBluetoothConnected)) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", false);
                } else if (opt == 1) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", true);
                }
                if (opt_b == 2 || (opt_b == 3 && !isBluetoothConnected)) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth_handsfree_battery", false);
                } else if (opt_b == 1) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth_handsfree_battery", true);
                }
            }
        });
    }
}
