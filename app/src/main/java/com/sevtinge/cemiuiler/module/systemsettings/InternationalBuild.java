package com.sevtinge.cemiuiler.module.systemsettings;

import android.os.Bundle;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;
import miui.os.Build;

public class InternationalBuild extends BaseHook {
    @Override
    public void init() {
        XposedHelpers.setStaticBooleanField(Build.class, "IS_INTERNATIONAL_BUILD", true);
    }
}