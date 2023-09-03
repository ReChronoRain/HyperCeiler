package com.sevtinge.cemiuiler.module.hook.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedBridge;

public class IsDetailLog extends BaseHook {
    @Override
    public void init() {
        if (mPrefsMap.getBoolean("settings_disable_detailed_log"))
            XposedBridge.log("Cemiuiler: Detail log is disabled.");
    }
}
