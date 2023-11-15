package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class ModifySystemVersion extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.device.MiuiAboutPhoneUtils", "getOsVersionCode", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(mPrefsMap.getString("various_updater_miui_version", "1.0.0.0"));
            }
        });
    }
}
