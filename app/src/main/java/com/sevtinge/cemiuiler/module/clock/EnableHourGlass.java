package com.sevtinge.cemiuiler.module.clock;

import android.content.Context;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;

public class EnableHourGlass extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.deskclock.util.Util", "isHourGlassEnable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
