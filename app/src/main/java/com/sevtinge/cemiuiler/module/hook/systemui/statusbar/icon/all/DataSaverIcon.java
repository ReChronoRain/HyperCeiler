package com.sevtinge.cemiuiler.module.hook.systemui.statusbar.icon.all;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DataSaverIcon extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy",
            "onDataSaverChanged",
            boolean.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_data_saver", 0);
                    if (opt == 1) {
                        param.args[0] = true;
                    } else if (opt == 2) {
                        param.args[0] = false;
                    }
                }
            }
        );
    }
}
