package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class LanguageMenuShowAllApps extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("android.util.FeatureFlagUtils", "isEnabled", Context.class, String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (param.args[1] == "settings_app_locale_opt_in_enabled") param.setResult(false);
            }
        });
    }
}
