package com.sevtinge.hyperceiler.hook.module.hook.powerkeeper;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ConservativeMillet extends BaseHook {
    @Override
    public void init() {
        // from https://github.com/kooritea/fcmfix/blob/master/app/src/main/java/com/kooritea/fcmfix/xposed/PowerkeeperFix.java
        Class<?> MilletConfig = XposedHelpers.findClassIfExists("com.miui.powerkeeper.millet.MilletConfig", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(MilletConfig, "isGlobal", true);
    }
}
