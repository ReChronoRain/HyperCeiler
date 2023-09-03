package com.sevtinge.cemiuiler.module.hook.systemsettings;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class NewNFCPage extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.utils.SettingsFeatures", "isNeedShowMiuiNFC", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
