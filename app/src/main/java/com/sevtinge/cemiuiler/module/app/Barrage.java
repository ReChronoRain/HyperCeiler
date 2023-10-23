package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.hook.barrage.*;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class Barrage extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(AnyBarrage.INSTANCE, mPrefsMap.getBoolean("barrage_any_barrage"));
        initHook(CustomBarrageLength.INSTANCE, mPrefsMap.getInt("barrage_custom_barrage_length", 36) != 36);
    }
}
