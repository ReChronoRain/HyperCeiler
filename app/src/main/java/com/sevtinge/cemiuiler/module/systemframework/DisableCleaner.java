package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;

public class DisableCleaner extends BaseHook {
    @Override
    public void init() {
        XposedHelpers.setStaticBooleanField(findClassIfExists("android.os.spc.PressureStateSettings"), "PROCESS_CLEANER_ENABLED", false);
    }
}
