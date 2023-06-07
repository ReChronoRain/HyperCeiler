package com.sevtinge.cemiuiler.module.systemui;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class NotificationFreeform extends BaseHook {

    Class<?> mNotificationCls;

    @Override
    public void init() {

        mNotificationCls = findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager");

        findAndHookMethod(mNotificationCls, "canSlide", String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
