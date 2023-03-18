package com.sevtinge.cemiuiler.module.systemui.statusbar;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

public class NetWorkSpeed extends BaseHook {

    @Override
    public void init() {
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_update_spacing")) {
            Helpers.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, "postUpdateNetworkSpeedDelay", long.class, new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    long originInterval = (long) param.args[0];
                    if (originInterval == 4000L) {
                        long newInterval = mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 4) * 1000L;
                        param.args[0] = newInterval;
                    }
                }
            });
        }
    }
}
