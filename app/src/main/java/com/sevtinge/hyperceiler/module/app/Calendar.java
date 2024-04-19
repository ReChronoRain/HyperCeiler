package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.hook.calendar.UnlockSubscription;

@HookExpand(pkg = "com.android.calendar", isPad = false, tarAndroid = 34)
public class Calendar extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockSubscription(), mPrefsMap.getBoolean("calendar_unlock_subscription"));
    }
}
