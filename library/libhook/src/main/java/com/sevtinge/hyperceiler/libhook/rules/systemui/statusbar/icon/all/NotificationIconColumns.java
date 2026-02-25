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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all;


import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NotificationIconColumns extends BaseHook {

    private Context mContext;
    private int mCurrentUserId;

    @Override
    public void init() {
        int maxIconsNum = PrefsBridge.getInt("system_ui_status_bar_notification_icon_maximum", 1);
        initHooks(maxIconsNum);
    }

    private void initHooks(int maxIconsNum) {
        String observerClass = isMoreAndroidVersion(36)
            ? "com.android.systemui.statusbar.policy.StatusBarIconObserver"
            : "com.android.systemui.statusbar.policy.NotificationIconObserver";

        hookAllConstructors(observerClass, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                mCurrentUserId = (int) getObjectField(param.getThisObject(), "mCurrentUserId");
                registerObserver(mContext);
            }
        });

        findAndHookMethod(observerClass + "$1", "onUserChanged", int.class, Context.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                mCurrentUserId = (int) param.getArgs()[0];
                updateSettingsState();
            }
        });

        IMethodHook setMaxIconsHook = new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                setObjectField(param.getThisObject(), "mMaxIcons", maxIconsNum);
            }
        };

        String containerClass = "com.android.systemui.statusbar.phone.NotificationIconContainer";
        findAndHookMethod(containerClass, "calculateIconXTranslations", setMaxIconsHook);
        findAndHookMethod(containerClass, "onMeasure", int.class, int.class, setMaxIconsHook);
    }

    private void registerObserver(Context context) {
        ContentObserver observer = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateSettingsState();
            }
        };

        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("status_bar_show_notification_icon"),
            false, observer
        );
        updateSettingsState();
    }

    private void updateSettingsState() {
        getSettingsInt(mContext, mCurrentUserId);
    }

    private void getSettingsInt(Context context, int userId) {
        try {
            callStaticMethod(Settings.System.class, "getIntForUser",
                context.getContentResolver(), "status_bar_show_notification_icon", 1, userId);
        } catch (Throwable ignored) {
        }
    }
}

