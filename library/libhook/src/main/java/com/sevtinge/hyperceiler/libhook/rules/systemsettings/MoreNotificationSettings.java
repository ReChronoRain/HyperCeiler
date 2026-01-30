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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class MoreNotificationSettings extends BaseHook {

    private static final String[] VISIBLE_PREF_KEYS = {"importance", "badge", "allow_keyguard"};

    @Override
    public void init() {
        Class<?> baseNotificationSettings = findClassIfExists(
            "com.android.settings.notification.BaseNotificationSettings", getClassLoader());
        Class<?> channelNotificationSettings = findClassIfExists(
            "com.android.settings.notification.ChannelNotificationSettings", getClassLoader());

        if (baseNotificationSettings == null || channelNotificationSettings == null) {
            XposedLog.e(TAG, getPackageName(), "Required classes not found");
            return;
        }

        hookSetPrefVisible(baseNotificationSettings);
        hookOnClickInfoItem();
        hookAllMethods(channelNotificationSettings, "setupChannelDefaultPrefs", new SetupChannelDefaultPrefsHook());
    }

    private void hookSetPrefVisible(Class<?> clazz) {
        hookAllMethods(clazz, "setPrefVisible", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Object pref = param.getArgs()[0];
                if (pref == null) return;

                String prefKey = (String) callMethod(pref, "getKey");
                for (String key : VISIBLE_PREF_KEYS) {
                    if (key.equals(prefKey)) {
                        param.getArgs()[1] = true;
                        break;
                    }
                }
            }
        });
    }

    private void hookOnClickInfoItem() {
        hookAllMethods("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow",
            "onClickInfoItem", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    try {
                        Context context = (Context) param.getArgs()[0];
                        Object parent = getObjectField(param.getThisObject(), "mParent");
                        Object entry = callMethod(parent, "getEntry");
                        Object channel = callMethod(entry, "getChannel");
                        String channelId = (String) callMethod(channel, "getId");

                        if ("miscellaneous".equals(channelId)) return;

                        Object notification = callMethod(entry, "getSbn");

                        Class<?> notifUtil = findClassIfExists(
                            "com.android.systemui.miui.statusbar.notification.NotificationUtil",
                            context.getClassLoader());
                        if (notifUtil != null) {
                            Boolean isHybrid = (Boolean) callStaticMethod(notifUtil, "isHybrid", notification);
                            if (Boolean.TRUE.equals(isHybrid)) return;
                        }

                        Intent intent = createIntent(notification, channelId);
                        callMethod(context, "startActivityAsUser", intent, android.os.Process.myUserHandle());
                        param.setResult(null);// 关闭面板
                        ClassLoader cl = context.getClassLoader();
                        Class<?> dependencyClass = findClass("com.android.systemui.Dependency", cl);
                        Class<?> modalControllerClass = findClass(
                            "com.android.systemui.statusbar.notification.modal.ModalController", cl);
                        Object modalController = callStaticMethod(dependencyClass, "get", modalControllerClass);
                        callMethod(modalController, "animExitModelCollapsePanels");

                    } catch (Throwable t) {
                        XposedLog.e(TAG, getPackageName(), "onClickInfoItem hook error", t);
                    }
                }
            });
    }

    private class SetupChannelDefaultPrefsHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            try {
                Object thisObject = param.getThisObject();
                Object pref = callMethod(thisObject, "findPreference", "importance");

                if (pref == null) {
                    XposedLog.w(TAG, getPackageName(), "importance preference not found");
                    return;
                }

                setObjectField(thisObject, "mImportance", pref);

                Object backupImportanceObj = getObjectField(thisObject, "mBackupImportance");
                int backupImportance = backupImportanceObj != null ? (int) backupImportanceObj : 0;

                if (backupImportance <= 0) return;

                int index = (int) callMethod(pref, "findSpinnerIndexOfValue", String.valueOf(backupImportance));
                if (index > -1) {
                    callMethod(pref, "setValueIndex", index);
                }

                // 创建监听器
                Object listener = createImportanceListener(thisObject);
                if (listener != null) {
                    callMethod(pref, "setOnPreferenceChangeListener", listener);
                }

            } catch (Throwable t) {
                XposedLog.e(TAG, getPackageName(), "setupChannelDefaultPrefs hook error", t);
            }
        }
    }

    private Object createImportanceListener(Object settings) {
        try {
            Class<?> listenerClass = findClassIfExists(
                "androidx.preference.Preference$OnPreferenceChangeListener", getClassLoader());
            if (listenerClass == null) return null;

            InvocationHandler handler = (proxy, method, args) -> {
                if (!"onPreferenceChange".equals(method.getName())) {
                    return true;
                }

                try {
                    int importance = Integer.parseInt((String) args[1]);
                    setObjectField(settings, "mBackupImportance", importance);

                    NotificationChannel channel = (NotificationChannel) getObjectField(settings, "mChannel");
                    if (channel != null) {
                        channel.setImportance(importance);
                        callMethod(channel, "lockFields", 4);

                        Object backend = getObjectField(settings, "mBackend");
                        String pkg = (String) getObjectField(settings, "mPkg");
                        Object uidObj = getObjectField(settings, "mUid");
                        int uid = uidObj != null ? (int) uidObj : 0;

                        callMethod(backend, "updateChannel", pkg, uid, channel);
                        callMethod(settings, "updateDependents", false);
                    }
                } catch (Throwable t) {
                    XposedLog.e(TAG, getPackageName(), "onPreferenceChange error", t);
                }

                return true;
            };

            return Proxy.newProxyInstance(getClassLoader(), new Class[]{listenerClass}, handler);

        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "createImportanceListener error", t);
            return null;
        }
    }

    private Intent createIntent(Object notification, String channelId) {
        String pkgName = (String) callMethod(notification, "getPackageName");
        int uid = (int) callMethod(notification, "getAppUid");

        Bundle bundle = new Bundle();
        bundle.putString("android.provider.extra.CHANNEL_ID", channelId);
        bundle.putString("package", pkgName);
        bundle.putInt("uid", uid);
        bundle.putString("miui.targetPkg", pkgName);

        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(":android:show_fragment", "com.android.settings.notification.ChannelNotificationSettings");
        intent.putExtra(":android:show_fragment_args", bundle);
        intent.setClassName("com.android.settings", "com.android.settings.SubSettings");

        return intent;
    }
}
