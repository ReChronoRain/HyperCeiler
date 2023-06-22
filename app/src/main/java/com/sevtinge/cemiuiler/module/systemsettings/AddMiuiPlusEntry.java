package com.sevtinge.cemiuiler.module.systemsettings;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class AddMiuiPlusEntry extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.connection.MiMirrorController", "isMirrorSupported", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}







