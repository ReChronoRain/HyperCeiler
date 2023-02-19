package com.sevtinge.cemiuiler.module.systemui.statusbar;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NotificationIconColumns extends BaseHook {

    Class<?> mNotificationIconContainer;

    @Override
    public void init() {
        mNotificationIconContainer = findClassIfExists("com.android.systemui.statusbar.phone.NotificationIconContainer");

        findAndHookMethod(mNotificationIconContainer, "miuiShowNotificationIcons", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean isShowNotificationIcons = (boolean) param.args[0];
                if (isShowNotificationIcons) {
                    int MAX_DOTS = mPrefsMap.getInt("system_ui_status_bar_notification_dots_maximum", 3);
                    int MAX_STATIC_ICONS = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 3);

                    if (MAX_DOTS > 0) {
                        XposedHelpers.setIntField(param.thisObject, "MAX_DOTS", MAX_DOTS);
                    }
                    if (MAX_STATIC_ICONS > 0) {
                        MAX_STATIC_ICONS = MAX_STATIC_ICONS == 16 ? 999 : MAX_STATIC_ICONS;
                        XposedHelpers.setIntField(param.thisObject, "MAX_STATIC_ICONS", MAX_STATIC_ICONS);
                        XposedHelpers.setIntField(param.thisObject, "MAX_VISIBLE_ICONS_ON_LOCK", MAX_STATIC_ICONS);
                    }
                } else {
                    XposedHelpers.setIntField(param.thisObject, "MAX_DOTS", 0);
                    XposedHelpers.setIntField(param.thisObject, "MAX_STATIC_ICONS", 0);
                    XposedHelpers.setIntField(param.thisObject, "MAX_VISIBLE_ICONS_ON_LOCK", 0);
                }
                XposedHelpers.callMethod(param.thisObject, "updateState");
                param.setResult(null);
            }
        });
    }
}
