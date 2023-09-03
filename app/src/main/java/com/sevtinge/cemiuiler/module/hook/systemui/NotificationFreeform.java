package com.sevtinge.cemiuiler.module.hook.systemui;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidU;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;

public class NotificationFreeform extends BaseHook {
    @Override
    public void init() {
        if (isAndroidU()) {
            findAndHookMethod(findClassIfExists("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow"), "updateMiniWindowBar", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    XposedHelpers.setObjectField(param.thisObject, "mCanSlide", true);
                }
            });
        } else {
            findAndHookMethod(findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager"), "canSlide", String.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
    }
}
