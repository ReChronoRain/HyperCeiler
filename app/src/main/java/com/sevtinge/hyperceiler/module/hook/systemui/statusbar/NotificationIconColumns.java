package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NotificationIconColumns extends BaseHook {

    public Context mContext;
    public int mCurrentUserId;
    public boolean mShowNotificationIcons;
    // public boolean z;

    @Override
    public void init() {
        if (!isAndroidVersion(30)) {
            int maxIconsNum = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 3);
            int maxDotsNum = mPrefsMap.getInt("system_ui_status_bar_notification_dots_maximum", 3);
            if (isAndroidVersion(34) && isMoreHyperOSVersion(1f)) {
                mAndroidU(maxIconsNum, maxDotsNum);
                // logE(TAG, "is hyper");
            } else {
                mAndroidS(maxIconsNum, maxDotsNum);
            }
            // mAndroidU(maxIconsNum, maxDotsNum);
        }
    }

    public void mAndroidU(int maxIconsNum, int maxDotsNum) {
        hookAllConstructors("com.android.systemui.statusbar.policy.NotificationIconObserver",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mCurrentUserId = (int) XposedHelpers.getObjectField(param.thisObject, "mCurrentUserId");
                    mShowNotificationIcons = getSettings(mContext, mCurrentUserId) == 1;
                    listening(mContext);
                    // mShowNotificationIcons = (boolean) XposedHelpers.getObjectField(param.thisObject, "mShowNotificationIcons");
                    // logE("NotificationIconColumns", "im get mContext: " + mContext + " mShowNotificationIcons: " + mShowNotificationIcons);
                }
            }
        );

        findAndHookMethod("com.android.systemui.statusbar.policy.NotificationIconObserver$1",
            "onUserChanged", int.class, Context.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    mCurrentUserId = (int) param.args[0];
                }
            }
        );

        /*findAndHookMethod("com.android.systemui.statusbar.policy.NotificationIconObserver$2",
            "onChange", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    // mShowNotificationIcons = (boolean) XposedHelpers.getObjectField(param.thisObject, "mShowNotificationIcons");
                    // if (mShowNotificationIcons != z) {
                    //     mShowNotificationIcons = z;
                    // }
                    // Settings.System.getInt()
                    // logE("NotificationIconColumns", " mShowNotificationIcons: " + mShowNotificationIcons);
                }
            }
        );*/

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
            "calculateIconXTranslations", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (mShowNotificationIcons) {
                        XposedHelpers.setObjectField(param.thisObject, "mMaxDots", maxDotsNum);
                        XposedHelpers.setObjectField(param.thisObject, "mMaxStaticIcons", maxIconsNum);
                        // logE("NotificationIconColumns", "im run 1");
                    } else {
                        XposedHelpers.setObjectField(param.thisObject, "mMaxDots", 0);
                        XposedHelpers.setObjectField(param.thisObject, "mMaxStaticIcons", 0);
                        // logE("NotificationIconColumns", "im run 2");
                    }
                }
            }
        );

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
            "calculateWidthFor", float.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (mShowNotificationIcons) {
                        XposedHelpers.setObjectField(param.thisObject, "mMaxIconsOnLockscreen", maxIconsNum);
                        // logE("NotificationIconColumns", "im run 3");
                    } else {
                        XposedHelpers.setObjectField(param.thisObject, "mMaxIconsOnLockscreen", 0);
                        // logE("NotificationIconColumns", "im run 4");
                    }
                }
            }
        );
    }

    public void listening(Context context) {
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                mShowNotificationIcons = getSettings(context, mCurrentUserId) == 1;
                // logE("NotificationIconColumns", "listening: " + mShowNotificationIcons);
            }
        };
        // XposedHelpers.setAdditionalInstanceField(param.thisObject, "mContentObserver", contentObserver);
        XposedHelpers.callMethod(XposedHelpers.callMethod(context, "getContentResolver"),
            "registerContentObserver",
            Settings.System.getUriFor("status_bar_show_notification_icon"), false,
            contentObserver, -1);
    }

    public int getSettings(Context context, int mCurrentUserId) {
        try {
            Class<?>[] classes = {ContentResolver.class, String.class, int.class, int.class};
            return (int) XposedHelpers.callStaticMethod(findClass("android.provider.Settings$System"),
                "getIntForUser",
                classes,
                context.getContentResolver(), "status_bar_show_notification_icon", 1, mCurrentUserId
            );
            // return Settings.System.getInt(context.getContentResolver(), "status_bar_show_notification_icon");
        } catch (Throwable e) {
            logE("NotificationIconColumns", "No found system: " + e);
            return -1;
        }
    }

    public void mAndroidS(int maxIconsNum, int maxDotsNum) {
        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
            "miuiShowNotificationIcons", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if ((boolean) param.args[0]) {
                        XposedHelpers.setObjectField(param.thisObject, "MAX_DOTS", maxDotsNum);
                        XposedHelpers.setObjectField(param.thisObject, "MAX_STATIC_ICONS", maxIconsNum);
                        if (isAndroidVersion(33)) {
                            XposedHelpers.setObjectField(param.thisObject, "MAX_ICONS_ON_LOCKSCREEN", maxIconsNum);
                        } else {
                            XposedHelpers.setObjectField(param.thisObject, "MAX_VISIBLE_ICONS_ON_LOCK", maxIconsNum);
                        }
                    } else {
                        XposedHelpers.setObjectField(param.thisObject, "MAX_DOTS", 0);
                        XposedHelpers.setObjectField(param.thisObject, "MAX_STATIC_ICONS", 0);
                        if (isAndroidVersion(33)) {
                            XposedHelpers.setObjectField(param.thisObject, "MAX_ICONS_ON_LOCKSCREEN", 0);
                        } else {
                            XposedHelpers.setObjectField(param.thisObject, "MAX_VISIBLE_ICONS_ON_LOCK", 0);
                        }
                    }
                    XposedHelpers.callMethod(param.thisObject, "updateState");
                    param.setResult(null);
                }
            }
        );
    }
}
