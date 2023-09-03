package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import android.content.Context;
import android.os.PowerManager;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class MuteVisibleNotifications extends BaseHook {

    @Override
    public void init() {
        Helpers.hookAllMethods("com.android.systemui.statusbar.notification.policy.NotificationAlertController", lpparam.classLoader, "buzzBeepBlink", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    if (powerMgr.isInteractive()) {
                        param.setResult(null);
                    }
                }
            }
        );
    }
}
