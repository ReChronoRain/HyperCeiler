/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

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

    @Override
    public void init() {
        int maxIconsNum = mPrefsMap.getInt("system_ui_status_bar_notification_icon_maximum", 1);
        mHyperOS(maxIconsNum);
    }

    public void mHyperOS(int maxIconsNum) {
        hookAllConstructors("com.android.systemui.statusbar.policy.NotificationIconObserver",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        mCurrentUserId = (int) XposedHelpers.getObjectField(param.thisObject, "mCurrentUserId");
                        mShowNotificationIcons = getSettings(mContext, mCurrentUserId) == 1;
                        listening(mContext);
                    }
                }
        );

        findAndHookMethod("com.android.systemui.statusbar.policy.NotificationIconObserver$1",
                "onUserChanged", int.class, Context.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        mCurrentUserId = (int) param.args[0];
                    }
                }
        );

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
                "calculateIconXTranslations",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isMoreAndroidVersion(35)) {
                            XposedHelpers.setObjectField(param.thisObject, "mMaxIcons", maxIconsNum);
                        } else {
                            XposedHelpers.setObjectField(param.thisObject, "mMaxStaticIcons", maxIconsNum);
                            XposedHelpers.setObjectField(param.thisObject, "mIsStaticLayout", true);
                        }
                    }
                }
        );

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
                "onMeasure", int.class, int.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isMoreAndroidVersion(35)) {
                            XposedHelpers.setObjectField(param.thisObject, "mMaxIcons", maxIconsNum);
                        } else {
                            XposedHelpers.setObjectField(param.thisObject, "mMaxStaticIcons", maxIconsNum);
                            XposedHelpers.setObjectField(param.thisObject, "mIsStaticLayout", true);
                        }
                    }
                }
        );

        if (!isMoreAndroidVersion(35)) {
            findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconContainer",
                    "calculateWidthFor", float.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            XposedHelpers.setObjectField(param.thisObject, "mMaxIconsOnLockscreen", maxIconsNum);
                        }
                    }
            );
        }
    }

    public void listening(Context context) {
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                mShowNotificationIcons = getSettings(context, mCurrentUserId) == 1;
            }
        };
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
        } catch (Throwable e) {
            logE("NotificationIconColumns", "No found system: " + e);
            return -1;
        }
    }
}
