package com.sevtinge.cemiuiler.module.systemui.statusbar;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BluetoothIcon extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarPolicy", "updateBluetooth", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0);
                boolean isBluetoothConnected = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mBluetooth"), "isBluetoothConnected");
                Object mIconController = XposedHelpers.getObjectField(param.thisObject, "mIconController");
                if (opt == 2 || (opt == 3 && !isBluetoothConnected)) {
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", false);
                } else if (opt == 1){
                    XposedHelpers.callMethod(mIconController, "setIconVisibility", "bluetooth", true);
                }
            }
        });
    }
}
