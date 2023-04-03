package com.sevtinge.cemiuiler.module.systemsettings;

import android.os.Bundle;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;
import miui.os.Build;

public class QuickInstallPermission extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.SettingsActivity", "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticBooleanField(Build.class, "IS_INTERNATIONAL_BUILD", true);
            }
        });
    }
}
