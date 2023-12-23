package com.sevtinge.hyperceiler.module.hook.aod;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class UnlockAodAon extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> mAodUtils = findClassIfExists("com.miui.aod.Utils");
        XposedHelpers.setStaticBooleanField(mAodUtils, "SUPPORT_AOD_AON", true);
    }
}
