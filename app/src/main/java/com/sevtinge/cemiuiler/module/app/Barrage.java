package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.hook.barrage.AnyBarrage;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class Barrage extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(AnyBarrage.INSTANCE, mPrefsMap.getBoolean("barrage_any_barrage"));
    }
}
